import hashlib
import json
import re
from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path
from tempfile import TemporaryDirectory
from typing import Any

from app.core.graph.utils.token_counter import count_tokens

from .models import EvidenceRecord

DEFAULT_SINGLE_TOOL_MAX_TOKENS = 4_915
DEFAULT_RECORD_BUDGET_TOKENS = 19_660


@dataclass(frozen=True, slots=True)
class _Artifact:
    path: Path
    source: str
    tool: str
    query: dict[str, Any]
    sha256: str
    size_bytes: int
    token_count: int


class EvidenceStore:
    """Request-scoped evidence records and spilled raw results, keyed by source.

    One store serves the whole request: the central agent's tools write into it and the
    graph projects source status, catalog and limitations out of it. The record budget is
    shared request-wide — there is no per-source slice, because the agent decides how many
    calls each source gets.
    """

    def __init__(
        self,
        *,
        storage_dir: Path | str | None = None,
        model_name: str = "gpt-4",
        single_tool_max_tokens: int = DEFAULT_SINGLE_TOOL_MAX_TOKENS,
        record_budget_tokens: int = DEFAULT_RECORD_BUDGET_TOKENS,
    ):
        self._owned_temp_dir = TemporaryDirectory(prefix="rca-evidence-") if storage_dir is None else None
        storage_dir = storage_dir or self._owned_temp_dir.name
        self.storage_dir = Path(storage_dir).resolve()
        self.storage_dir.mkdir(parents=True, exist_ok=True)
        self.model_name = model_name
        self.single_tool_max_tokens = single_tool_max_tokens
        self.record_budget_tokens = record_budget_tokens
        self._artifacts: dict[str, _Artifact] = {}
        self._records: dict[str, EvidenceRecord] = {}
        self._used_record_tokens = 0
        self._spilled_refs: set[str] = set()
        self._discovery_refs: set[str] = set()
        self._inspected_refs: set[str] = set()
        self._admission_errors: dict[str, str] = {}
        self._unavailable_sources: set[str] = set()

    def capture(
        self,
        *,
        source: str,
        tool: str,
        query: dict[str, Any],
        value: Any,
    ) -> dict[str, Any]:
        normalized, canonical = _canonical(value)
        reference, digest = _reference(source, tool, query, canonical)
        record = EvidenceRecord(
            evidence_id=reference,
            source=source,
            observation=canonical,
            tool=tool,
            query=dict(query),
        )
        inline = {
            "records": [record.model_dump(mode="json")],
            "truncated": False,
        }
        if self._fits(inline):
            if (error := self._admit(record)) is None:
                return inline
            return self._error(error)

        return self._spill(
            reference=reference,
            digest=digest,
            source=source,
            tool=tool,
            query=query,
            normalized=normalized,
            canonical=canonical,
            recording=True,
        )

    def capture_discovery(
        self,
        *,
        source: str,
        value: Any,
    ) -> Any:
        normalized, canonical = _canonical(value)
        if self._fits(normalized):
            return normalized
        reference, digest = _reference(source, "discovery", {}, canonical)
        return self._spill(
            reference=reference,
            digest=digest,
            source=source,
            tool="discovery",
            query={},
            normalized=normalized,
            canonical=canonical,
            recording=False,
        )

    def inspect(
        self,
        evidence_ref: str,
        *,
        path: str = "",
        offset: int = 0,
        limit: int = 10,
    ) -> dict[str, Any]:
        artifact = self._artifacts.get(evidence_ref)
        if artifact is None:
            return self._error("evidence_ref_not_found")
        if offset < 0:
            return self._error("invalid_offset")
        if not 1 <= limit <= 100:
            return self._error("invalid_limit")

        try:
            artifact_path = artifact.path.resolve()
            if not artifact_path.is_relative_to(self.storage_dir):
                self._unavailable_sources.add(artifact.source)
                return self._error("evidence_path_invalid")
            raw = artifact_path.read_text(encoding="utf-8")
        except FileNotFoundError:
            self._unavailable_sources.add(artifact.source)
            return self._error("evidence_file_not_found")
        except OSError:
            self._unavailable_sources.add(artifact.source)
            return self._error("evidence_read_failed")

        try:
            value = json.loads(raw)
        except json.JSONDecodeError:
            self._unavailable_sources.add(artifact.source)
            return self._error("evidence_invalid_json")

        try:
            selected = _resolve_pointer(value, path)
        except (KeyError, IndexError, TypeError, ValueError):
            # A wrong pointer is a navigation mistake, not lost evidence: hand back the
            # real paths so the next call can land instead of ending the source.
            return self._error("evidence_path_not_found", outline=_outline(value))

        _, selected_canonical = _canonical(selected)
        base = {
            "error": None,
            "evidence_ref": evidence_ref,
            "path": path,
            "token_count": count_tokens(selected_canonical, self.model_name),
        }
        view = _view(
            selected,
            path,
            offset,
            limit,
            lambda candidate: self._fits({**base, **candidate}),
            self.model_name,
        )
        result = {**base, **view}
        if not self._fits(result):
            # Asking for too much is recoverable — the evidence is still there, so the
            # source must not be marked exhausted over one oversized request.
            return self._error(
                "inspection_response_too_large",
                next_action="narrow_path_or_limit",
                outline=_outline(selected, max_entries=8, max_depth=2),
            )

        if evidence_ref in self._discovery_refs:
            return result

        observation = _serialize({key: value for key, value in result.items() if key != "error"})
        view_digest = hashlib.sha256(f"{evidence_ref}:{path}:{offset}:{observation}".encode()).hexdigest()[:16]
        record = EvidenceRecord(
            evidence_id=f"{artifact.source}:{view_digest}",
            source=artifact.source,
            observation=observation,
            tool=artifact.tool,
            query=artifact.query,
        )
        if error := self._admit(record):
            return self._error(error)
        self._inspected_refs.add(evidence_ref)
        return result

    def records_for(self, source: str) -> list[EvidenceRecord]:
        return [record for record in self._records.values() if record.source == source]

    def has_uninspected(self, source: str) -> bool:
        return any(
            reference not in self._inspected_refs and self._artifacts[reference].source == source
            for reference in self._spilled_refs
        )

    def admission_error(self, source: str) -> str | None:
        return self._admission_errors.get(source)

    def has_unavailable(self, source: str) -> bool:
        return source in self._unavailable_sources

    def _spill(
        self,
        *,
        reference: str,
        digest: str,
        source: str,
        tool: str,
        query: dict[str, Any],
        normalized: Any,
        canonical: str,
        recording: bool,
    ) -> dict[str, Any]:
        path = (self.storage_dir / f"{digest}.json").resolve()
        if not path.is_relative_to(self.storage_dir):
            self._unavailable_sources.add(source)
            return self._error(
                "evidence_storage_failed",
                inspection_required=False,
                next_action="narrow_query",
            )
        try:
            if reference not in self._artifacts:
                path.write_text(canonical, encoding="utf-8")
                self._artifacts[reference] = _Artifact(
                    path=path,
                    source=source,
                    tool=tool,
                    query=dict(query),
                    sha256=hashlib.sha256(canonical.encode()).hexdigest(),
                    size_bytes=len(canonical.encode()),
                    token_count=count_tokens(canonical, self.model_name),
                )
        except OSError:
            self._unavailable_sources.add(source)
            return self._error(
                "evidence_storage_failed",
                inspection_required=False,
                next_action="narrow_query",
            )

        artifact = self._artifacts[reference]
        if recording:
            self._spilled_refs.add(reference)
        else:
            self._discovery_refs.add(reference)
        base = {
            "evidence_ref": reference,
            "stored": True,
            "type": _kind(normalized),
            "token_count": artifact.token_count,
            "size_bytes": artifact.size_bytes,
            "sha256": artifact.sha256,
            "inspection_required": True,
            "outline": _outline(normalized),
        }
        if recording:
            # The result exceeded the per-call token budget: say so, say how big it is, and
            # say what to do — the raw rows are on disk for inspect_evidence either way.
            base.update(
                {
                    "truncated": True,
                    "rows_total": _rows_total(normalized),
                    "reason": "result_exceeds_context",
                    "suggestion": "Narrow the query or lower limit, then retry.",
                }
            )
        preview = _view(
            normalized,
            "",
            0,
            5,
            lambda candidate: self._fits({**base, "root_preview": candidate}),
            self.model_name,
        )
        result = {**base, "root_preview": preview}
        if self._fits(result):
            return result
        # The outline is worth more than the preview: keep the map, drop the sample.
        result["root_preview"] = {"kind": _kind(normalized)}
        if self._fits(result):
            return result
        result["outline"] = _outline(normalized, max_entries=4, max_depth=2)
        if self._fits(result):
            return result
        # A spill result that does not fit the per-call budget is worse than no map; the
        # pointer, the size and the truncation flag are what must survive.
        for key in ("sha256", "outline", "reason", "suggestion"):
            result.pop(key, None)
            if self._fits(result):
                return result
        return result

    def _admit(self, record: EvidenceRecord) -> str | None:
        if record.evidence_id in self._records:
            return None
        size = count_tokens(_serialize(record.model_dump(mode="json")), self.model_name)
        if self._used_record_tokens + size > self.record_budget_tokens:
            self._admission_errors[record.source] = "evidence_budget_exhausted"
            return "evidence_budget_exhausted"
        self._records[record.evidence_id] = record
        self._used_record_tokens += size
        return None

    def _fits(self, value: Any) -> bool:
        return count_tokens(_serialize(value), self.model_name) <= self.single_tool_max_tokens

    def _error(self, code: str, **details: Any) -> dict[str, Any]:
        result = {"error": code, **details}
        return result if self._fits(result) else {"error": code}


def _canonical(value: Any) -> tuple[Any, str]:
    canonical = _serialize(value)
    return json.loads(canonical), canonical


def _serialize(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
        default=str,
    )


def _reference(
    source: str,
    tool: str,
    query: dict[str, Any],
    canonical: str,
) -> tuple[str, str]:
    metadata = _serialize(
        {
            "source": source,
            "tool": tool,
            "query": query,
        }
    )
    digest = hashlib.sha256(f"{metadata}\0{canonical}".encode()).hexdigest()
    return f"{source}:{digest[:16]}", digest


def _rows_total(value: Any) -> int:
    """How many rows a payload carries: the length of its first list, else one."""
    if isinstance(value, list):
        return len(value)
    if isinstance(value, dict):
        for item in value.values():
            if isinstance(item, list):
                return len(item)
            if isinstance(item, dict):
                return _rows_total(item)
    return 1


def _kind(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, dict):
        return "object"
    if isinstance(value, list):
        return "array"
    if isinstance(value, str):
        return "string"
    return "number"


def _pointer_token(value: Any) -> str:
    return str(value).replace("~", "~0").replace("/", "~1")


def _outline(value: Any, *, max_entries: int = 12, max_depth: int = 4) -> list[dict[str, Any]]:
    """Map the shape of a payload as addressable JSON Pointer paths.

    A preview of the first few elements does not tell an agent where anything lives,
    so it has to guess pointers into a structure it has never seen — and one wrong
    guess used to end the source. This lists what is actually there.
    """
    entries: list[dict[str, Any]] = []
    queue: list[tuple[str, Any, int]] = [("", value, 0)]
    while queue and len(entries) < max_entries:
        path, node, depth = queue.pop(0)
        if isinstance(node, dict):
            keys = list(node)[:12]
            entries.append({"path": path or "", "kind": "object", "keys": keys})
            if depth < max_depth:
                queue.extend(
                    (f"{path}/{_pointer_token(key)}", node[key], depth + 1)
                    for key in keys
                    if isinstance(node[key], (dict, list))
                )
        elif isinstance(node, list):
            entries.append({"path": path or "", "kind": "array", "len": len(node)})
            if depth < max_depth and node and isinstance(node[0], (dict, list)):
                queue.append((f"{path}/0", node[0], depth + 1))
        else:
            # Scalars matter too: "a 40k-char string lives here" is what tells the
            # agent to window it with offset/limit rather than ask for the whole value.
            entry = {"path": path or "", "kind": _kind(node)}
            if isinstance(node, str):
                entry["len"] = len(node)
            entries.append(entry)
    return entries


def _resolve_pointer(value: Any, path: str) -> Any:
    if path == "":
        return value
    if not path.startswith("/"):
        raise ValueError("JSON Pointer must start with /")
    current = value
    for raw_token in path[1:].split("/"):
        if re.search(r"~(?:[^01]|$)", raw_token):
            raise ValueError("invalid JSON Pointer escape")
        token = raw_token.replace("~1", "/").replace("~0", "~")
        if isinstance(current, dict):
            current = current[token]
        elif isinstance(current, list):
            if not token.isdigit():
                raise ValueError("array index must be a non-negative integer")
            current = current[int(token)]
        else:
            raise TypeError("cannot traverse a scalar")
    return current


def _child(
    value: Any,
    path: str,
    *,
    label: str,
    label_value: Any,
    model_name: str,
) -> dict[str, Any]:
    _, canonical = _canonical(value)
    return {
        label: label_value,
        "path": path,
        "kind": _kind(value),
        "token_count": count_tokens(canonical, model_name),
        "value": value,
    }


def _page(
    kind: str,
    values: list[Any],
    path: str,
    offset: int,
    limit: int,
    fits: Callable[[dict[str, Any]], bool],
    model_name: str,
) -> dict[str, Any]:
    total = len(values)
    children = []
    result = {
        "kind": kind,
        "range": {"offset": offset, "returned": 0, "total": total},
        "children": children,
        "has_more": offset < total,
    }
    for position in range(offset, min(total, offset + limit)):
        if kind == "object":
            key, value = values[position]
            child = _child(
                value,
                f"{path}/{_pointer_token(key)}",
                label="key",
                label_value=key,
                model_name=model_name,
            )
        else:
            value = values[position]
            child = _child(
                value,
                f"{path}/{position}",
                label="index",
                label_value=position,
                model_name=model_name,
            )
        children.append(child)
        result["range"]["returned"] = len(children)
        result["has_more"] = offset + len(children) < total
        if fits(result):
            continue
        child.pop("value")
        if fits(result):
            continue
        children.pop()
        result["range"]["returned"] = len(children)
        result["has_more"] = offset + len(children) < total
        break
    return result


def _string_view(
    value: str,
    offset: int,
    limit: int,
    fits: Callable[[dict[str, Any]], bool],
) -> dict[str, Any]:
    requested = min(limit, max(0, len(value) - offset))
    low, high = 0, requested
    best = {
        "kind": "string",
        "range": {"offset": offset, "returned": 0, "total": len(value)},
        "value": "",
        "has_more": offset < len(value),
    }
    while low <= high:
        length = (low + high) // 2
        candidate = {
            "kind": "string",
            "range": {
                "offset": offset,
                "returned": length,
                "total": len(value),
            },
            "value": value[offset : offset + length],
            "has_more": offset + length < len(value),
        }
        if fits(candidate):
            best = candidate
            low = length + 1
        else:
            high = length - 1
    return best


def _view(
    value: Any,
    path: str,
    offset: int,
    limit: int,
    fits: Callable[[dict[str, Any]], bool],
    model_name: str,
) -> dict[str, Any]:
    if isinstance(value, dict):
        return _page(
            "object",
            list(value.items()),
            path,
            offset,
            limit,
            fits,
            model_name,
        )
    if isinstance(value, list):
        return _page("array", value, path, offset, limit, fits, model_name)
    if isinstance(value, str):
        return _string_view(value, offset, limit, fits)
    return {
        "kind": _kind(value),
        "range": {"offset": 0, "returned": 1, "total": 1},
        "value": value,
        "has_more": False,
    }
