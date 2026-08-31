import json
import logging
from datetime import UTC, datetime
from typing import Any

from langgraph.graph import END, START, StateGraph
from langgraph.runtime import Runtime

from app.core.graph.utils.token_counter import count_tokens

from .investigation import (
    InvestigationBudgetExhaustedError,
    RequestContextTooLargeError,
    build_investigation_payload,
    fit_investigation_payload,
    investigation_fixed_tokens,
)
from .models import (
    DraftEvidencePlan,
    IncidentScope,
    RcaAnalysisState,
    RcaResult,
    RcaRunContext,
)
from .specs import SOURCE_SPECS

logger = logging.getLogger(__name__)

_DEFAULT_QUERY = "Analyze the incident and identify the most probable evidence-backed cause."
_MAX_INVESTIGATION_ROUNDS = 1
_TRIM_NOTE_TOKENS = 64
_MAX_NOTE_CHARS = 2_000
# A source the agent chose not to query is not a gap; a source that could not be offered is.
_NOT_QUERIED = "not_queried"


class RcaGraphNodes:
    async def plan_evidence(
        self,
        state: RcaAnalysisState,
        runtime: Runtime[RcaRunContext],
    ) -> dict[str, Any]:
        context = runtime.context
        scope = IncidentScope.model_validate(state.get("scope") or {})
        scope = _scope_with_discovered_trace(scope, state.get("merged_evidence"))
        available = list(context.investigation_toolset.queryable_sources)
        reinvestigating = bool(state.get("evidence_gaps"))
        draft = await _draft_plan(context.llm, state, scope, available, context.investigation_toolset.ledger())
        hypotheses = list(dict.fromkeys([*state.get("hypotheses", []), *draft.hypotheses]))[:5]
        return {
            "query": state.get("query") or _DEFAULT_QUERY,
            "available_sources": available,
            "hypotheses": hypotheses,
            "investigation_round": (int(state.get("investigation_round", 0)) + 1 if reinvestigating else 0),
            "scope": scope.model_dump(mode="json"),
            "filters": state.get("filters") or {},
            "evidence_plan": validate_plan(draft, available),
        }

    async def investigate_evidence(
        self,
        state: RcaAnalysisState,
        runtime: Runtime[RcaRunContext],
    ) -> dict[str, Any]:
        """One central agent round, then the store projection that the graph works from."""
        context = runtime.context
        toolset = context.investigation_toolset
        config = context.analysis_config
        plan = state.get("evidence_plan") or {}
        limitations: list[dict[str, Any]] = []
        error_message: str | None = None
        blocked_reason: str | None = None
        notes: str | None = None

        runner = context.investigation_runner
        budget = context.budget
        previous = state.get("merged_evidence") or {}
        if runner is not None and toolset.queryable_sources:
            if budget is not None and budget.expired:
                limitations.append({"source": "investigation", "reason": "request_deadline_exceeded"})
            elif budget is not None and budget.remaining_model_calls == 0:
                limitations.append({"source": "investigation", "reason": "investigation_budget_exhausted"})
            else:
                scope = IncidentScope.model_validate(state.get("scope") or {})
                payload = build_investigation_payload(
                    query=state.get("query") or _DEFAULT_QUERY,
                    scope=scope,
                    hypotheses=list(state.get("hypotheses") or []),
                    hints=list(plan.get("hints") or []),
                    available_sources=list(toolset.queryable_sources),
                    prior_evidence_catalog=list(previous.get("evidence_catalog") or []),
                    prior_tool_calls=toolset.ledger(),
                    evidence_gaps=list(state.get("evidence_gaps") or []),
                    investigation_round=int(state.get("investigation_round", 0)),
                )
                model_name = str(config.get("model_name") or "gpt-4")
                try:
                    fitted = fit_investigation_payload(
                        payload,
                        max_tokens=int(config["context_window_tokens"]),
                        fixed_tokens=investigation_fixed_tokens(toolset, model_name),
                        model_name=model_name,
                    )
                except RequestContextTooLargeError as exc:
                    blocked_reason = error_message = exc.code
                else:
                    try:
                        result = await runner.ainvoke({"messages": [{"role": "user", "content": fitted["text"]}]})
                        notes = _final_note(result)
                    except InvestigationBudgetExhaustedError:
                        limitations.append({"source": "investigation", "reason": "investigation_budget_exhausted"})
                    except Exception as exc:
                        # Evidence already in the store survives; only this round's remainder is lost.
                        logger.warning("RCA investigation runner failed: %s", exc)
                        limitations.append({"source": "investigation", "reason": "investigation_runner_failed"})

        merged = _build_merged_evidence(toolset)
        merged["limitations"].extend(entry for entry in limitations if entry not in merged["limitations"])
        if blocked_reason:
            merged["synthesis_blocked_reason"] = blocked_reason
        if notes:
            merged["investigation_notes"] = notes[:_MAX_NOTE_CHARS]
        update: dict[str, Any] = {
            "merged_evidence": merged,
            "investigation_budget": {
                "model_calls": budget.remaining_model_calls if budget is not None else None,
                "tool_calls": budget.remaining_tool_calls if budget is not None else None,
                "expired": budget.expired if budget is not None else False,
            },
        }
        if error_message:
            update["error_message"] = error_message
        return update

    async def synthesize(
        self,
        state: RcaAnalysisState,
        runtime: Runtime[RcaRunContext],
    ) -> dict[str, Any]:
        config = runtime.context.analysis_config
        merged = _fit_synthesis_evidence(
            json.loads(json.dumps(state.get("merged_evidence") or {}, default=str)),
            max_tokens=int(config.get("synthesis_evidence_max_tokens", 26_214)),
            model_name=str(config.get("model_name") or "gpt-4"),
        )
        if reason := merged.get("synthesis_blocked_reason"):
            return {"analysis_result": None, "error_message": reason, "merged_evidence": merged}
        if not merged.get("evidence_catalog"):
            return {
                "analysis_result": None,
                "error_message": "No usable evidence was collected.",
                "merged_evidence": merged,
            }
        system_prompt = config.get(
            "synthesis_system_prompt",
            "Return a grounded RCA result from the supplied evidence only.",
        ).format(current_time=datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ"))
        system_prompt = (
            f"{system_prompt}\n\n"
            "Set conclusion_strength to CONFIRMED only when raw evidence records from two different sources "
            "support the causal claim without contradiction; use LIKELY for one grounded causal path and "
            "INCONCLUSIVE when evidence is sparse, indirect, or conflicting. For an INCONCLUSIVE result, "
            "put only concrete, collectable evidence gaps in next_checks.\n"
            "probable_cause explains the requested scope only; leave it empty when that scope shows no failure "
            "or when the requested name itself has no data (a near match is a related finding, not the scope). "
            "The summary's first sentence states what was observed for the requested scope, naming it verbatim; "
            "other services follow as related findings labelled with their own name."
        )
        last_error = "synthesis failed"
        for _ in range(2):
            try:
                result = await runtime.context.llm.with_structured_output(RcaResult).ainvoke(
                    [
                        {"role": "system", "content": system_prompt},
                        {
                            "role": "user",
                            "content": json.dumps(
                                {
                                    "query": state.get("query"),
                                    "scope": state.get("scope"),
                                    "candidate_hypotheses": state.get("hypotheses") or [],
                                    "investigation_round": state.get("investigation_round", 0),
                                    "merged_evidence": _synthesis_evidence(merged),
                                },
                                ensure_ascii=False,
                                default=str,
                            ),
                        },
                    ]
                )
                result = RcaResult.model_validate(result)
                scope = IncidentScope.model_validate(state.get("scope") or {})
                scope_defaults = {
                    "affected_service": scope.service_name,
                    "affected_endpoint": scope.endpoint,
                }
                result = result.model_copy(
                    update={
                        field: value
                        for field, value in scope_defaults.items()
                        if getattr(result, field) is None and value is not None
                    }
                )
                return {"analysis_result": result.model_dump(mode="json"), "merged_evidence": merged}
            except Exception as exc:
                last_error = str(exc)
        return {"analysis_result": None, "error_message": last_error, "merged_evidence": merged}

    async def validate_result(
        self,
        state: RcaAnalysisState,
        runtime: Runtime[RcaRunContext],
    ) -> dict[str, Any]:
        return _validate_result(
            state.get("analysis_result"),
            state.get("merged_evidence"),
            runtime.context.analysis_config,
        )


def _final_note(result: Any) -> str | None:
    messages = result.get("messages") if isinstance(result, dict) else None
    for message in reversed(messages or []):
        content = getattr(message, "content", None)
        if getattr(message, "type", "") == "ai" and isinstance(content, str) and content.strip():
            return content.strip()
        if isinstance(content, list):
            text = " ".join(
                part.get("text", "") for part in content if isinstance(part, dict) and part.get("type") == "text"
            ).strip()
            if text:
                return text
    return None


def _build_merged_evidence(toolset) -> dict[str, Any]:
    """Project the store into what synthesis and validation read.

    Records live in ``evidence_catalog`` only; per-source status and source-tagged
    limitations are what the validator needs, and the tool traces stay with the toolset
    for the operational log.
    """
    statuses = toolset.source_statuses()
    return {
        "sources": {item.source: item.status for item in statuses},
        "limitations": [{"source": item.source, "reason": reason} for item in statuses for reason in item.limitations],
        "evidence_catalog": [
            record.model_dump(mode="json")
            for item in statuses
            if item.status in {"OK", "PARTIAL"}
            for record in toolset.evidence_store.records_for(item.source)
        ],
        "discovered_trace_ids": sorted(set(toolset.discovered_trace_ids)),
    }


def _fit_synthesis_evidence(
    merged: dict[str, Any],
    *,
    max_tokens: int,
    model_name: str,
) -> dict[str, Any]:
    """Shrink the citable catalog until the synthesis payload fits.

    Blocking on overflow means collecting *more* evidence can make an analysis fail
    outright — the LLM then sees none of it. Dropping the tail of the noisiest source
    costs a few records and keeps the conclusion. Only a payload that cannot hold a
    single record still blocks.
    """

    def tokens(candidate: dict[str, Any]) -> int:
        return count_tokens(_compact_json(_synthesis_evidence(candidate)), model_name)

    if tokens(merged) <= max_tokens:
        return merged

    # The trim limitation is appended after fitting, so leave room for it.
    max_tokens = max(max_tokens - _TRIM_NOTE_TOKENS, 0)
    catalog = merged.get("evidence_catalog") or []
    # Price each record once against the empty-catalog payload instead of re-counting
    # the whole payload per record; the exact total is verified below.
    used = tokens({**merged, "evidence_catalog": []})
    kept: list[dict[str, Any]] = []
    for record in _interleave_by_source(catalog):
        size = count_tokens(_compact_json(record), model_name)
        if used + size > max_tokens:
            continue
        kept.append(record)
        used += size
    while kept and tokens({**merged, "evidence_catalog": kept}) > max_tokens:
        kept.pop()

    dropped = len(catalog) - len(kept)
    if not kept:
        merged["synthesis_blocked_reason"] = "synthesis_evidence_budget_exceeded"
        return merged

    # Preserve catalog order so evidence still reads trace -> log -> metric.
    kept_ids = {record.get("evidence_id") for record in kept}
    merged["evidence_catalog"] = [record for record in catalog if record.get("evidence_id") in kept_ids]
    merged["limitations"] = [
        *merged.get("limitations", []),
        {"source": "synthesis", "reason": f"evidence_trimmed_to_context:{dropped}"},
    ]
    return merged


def _interleave_by_source(catalog: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Round-robin over sources so a chatty one cannot crowd out the others."""
    by_source: dict[str, list[dict[str, Any]]] = {}
    for record in catalog:
        by_source.setdefault(record.get("source") or "", []).append(record)
    ordered: list[dict[str, Any]] = []
    while any(by_source.values()):
        for records in by_source.values():
            if records:
                ordered.append(records.pop(0))
    return ordered


def _compact_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), default=str)


def _synthesis_evidence(merged: dict[str, Any] | None) -> dict[str, Any]:
    merged = merged or {}
    return {
        key: merged.get(key, [] if key in {"evidence_catalog", "limitations"} else {})
        for key in ("evidence_catalog", "sources", "limitations")
    }


async def _draft_plan(
    llm,
    state: RcaAnalysisState,
    scope: IncidentScope,
    available_sources: list[str],
    prior_tool_calls: list[dict[str, Any]],
) -> DraftEvidencePlan:
    if llm is None:
        return DraftEvidencePlan()
    try:
        draft = await llm.with_structured_output(DraftEvidencePlan).ainvoke(
            [
                {
                    "role": "system",
                    "content": (
                        "Draft candidate hypotheses for a root-cause analysis and, for each, the investigation "
                        "focus and the telemetry source most likely to test or disprove it. Start with at least "
                        "two plausible hypotheses when the request is ambiguous. Use only the listed sources. "
                        "Do not write queries; a single investigation agent decides the actual tool calls. "
                        "During a follow-up round, focus on the listed evidence gaps and on what the prior "
                        "evidence and call ledger have not yet covered."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "query": state.get("query"),
                            "sources": {
                                source: SOURCE_SPECS[source]["summary"]
                                for source in available_sources
                                if source in SOURCE_SPECS
                            },
                            "scope": scope.model_dump(mode="json"),
                            "filters": state.get("filters") or {},
                            "previous_hypotheses": state.get("hypotheses") or [],
                            "evidence_gaps": state.get("evidence_gaps") or [],
                            "prior_evidence_catalog": [
                                {key: item.get(key) for key in ("evidence_id", "source", "tool")}
                                for item in ((state.get("merged_evidence") or {}).get("evidence_catalog") or [])
                            ],
                            "prior_tool_calls": prior_tool_calls,
                        },
                        ensure_ascii=False,
                        default=str,
                    ),
                },
            ]
        )
        return DraftEvidencePlan.model_validate(draft)
    except Exception:
        return DraftEvidencePlan()


def build_rca_graph(*, checkpointer=None):
    nodes = RcaGraphNodes()
    graph = StateGraph(RcaAnalysisState, context_schema=RcaRunContext)
    graph.add_node("plan_evidence", nodes.plan_evidence)
    graph.add_node("investigate_evidence", nodes.investigate_evidence)
    graph.add_node("synthesize", nodes.synthesize)
    graph.add_node("validate_result", nodes.validate_result)
    graph.add_edge(START, "plan_evidence")
    graph.add_edge("plan_evidence", "investigate_evidence")
    graph.add_edge("investigate_evidence", "synthesize")
    graph.add_edge("synthesize", "validate_result")
    graph.add_conditional_edges(
        "validate_result",
        route_after_validation,
        ["plan_evidence", END],
    )
    return graph.compile(checkpointer=checkpointer)


def _validate_result(result: dict | None, merged_evidence: dict | None, analysis_config: dict) -> dict[str, Any]:
    merged_evidence = merged_evidence or {}
    sources: dict[str, str] = merged_evidence.get("sources") or {}
    limitations = merged_evidence.get("limitations") or []
    usable_sources = {source for source, status in sources.items() if status in {"OK", "PARTIAL"}}
    catalog = {
        item["evidence_id"]: item
        for item in merged_evidence.get("evidence_catalog", [])
        if item.get("source") in usable_sources
    }
    threshold = analysis_config.get("partial_confidence_threshold", 0.4)
    confidence = result.get("confidence") if isinstance(result, dict) else None
    execution_reasons = _execution_reasons(result, sources, limitations)
    # An empty catalog because every queried source ran and found nothing is a finding, not
    # a broken analysis. Only a source that actually failed makes the run unusable.
    no_telemetry = all(status in {"NO_DATA", "SKIPPED"} for status in sources.values()) and "NO_DATA" in sources.values()
    if not catalog:
        execution_reasons.append("no telemetry data in the requested window" if no_telemetry else "no usable evidence")
        result = None
    conclusion_reasons: list[str] = []
    evidence_gaps: list[str] = []
    if isinstance(result, dict):
        result, supporting, contradiction_count = _ground_result_references(
            result,
            catalog,
            execution_reasons,
        )
        result, conclusion_reasons, evidence_gaps = _assess_conclusion(
            result,
            catalog,
            supporting,
            contradiction_count,
            threshold,
        )

    execution_reasons = list(dict.fromkeys(execution_reasons))
    conclusion_reasons = list(dict.fromkeys(conclusion_reasons))
    if catalog:
        status = "PARTIAL" if execution_reasons else "SUCCEEDED"
    else:
        status = "PARTIAL" if no_telemetry else "FAILED"
    return {
        "analysis_result": result,
        "evidence_gaps": evidence_gaps,
        "result_validation": {
            "status": status,
            "no_telemetry": not catalog and no_telemetry,
            "reasons": execution_reasons,
            "conclusion_reasons": conclusion_reasons,
            "confidence": result.get("confidence") if isinstance(result, dict) else confidence,
            "confidence_threshold": threshold,
            "evidence_count": len(sources),
            "usable_evidence_count": len(catalog),
            "conclusion_strength": (
                result.get("conclusion_strength") if isinstance(result, dict) else "INCONCLUSIVE"
            ),
        },
    }


def _execution_reasons(result: dict | None, sources: dict[str, str], limitations: list[Any]) -> list[str]:
    reasons = []
    if not result:
        reasons.append("missing result")
    by_source: dict[str, set[str]] = {}
    for limitation in limitations:
        if isinstance(limitation, dict):
            by_source.setdefault(str(limitation.get("source")), set()).add(str(limitation.get("reason")))
    for source, status in sources.items():
        if status in {"OK", "NO_DATA"}:
            # NO_DATA ran to completion — an empty window is a finding, not an execution gap.
            continue
        if status == "SKIPPED":
            # The agent choosing not to query a source is not a gap; a source that could
            # not be offered at all is.
            if by_source.get(source, set()) - {_NOT_QUERIED}:
                reasons.append(f"source unavailable: {source}")
            continue
        reasons.append(f"incomplete evidence: {source}")
    reasons.extend(f"investigation incomplete: {reason}" for reason in sorted(by_source.get("investigation", ())))
    return reasons


def _ground_result_references(
    result: dict[str, Any],
    catalog: dict[str, dict[str, Any]],
    execution_reasons: list[str],
) -> tuple[dict[str, Any], list[dict[str, Any]], int]:
    result = {**result}
    result["evidence"] = [
        grounded
        for item in result.get("evidence", [])
        if (grounded := _ground_reference(item, catalog, execution_reasons)) is not None
    ]
    grounded_hypotheses = []
    for hypothesis in result.get("hypotheses", []):
        grounded_hypothesis = {**hypothesis}
        for key in ("supporting_evidence", "contradicting_evidence"):
            grounded_hypothesis[key] = [
                grounded
                for evidence_id in hypothesis.get(key, [])
                if (grounded := _ground_hypothesis_reference(evidence_id, catalog, execution_reasons)) is not None
            ]
        grounded_hypotheses.append(grounded_hypothesis)
    result["hypotheses"] = grounded_hypotheses
    if result.get("probable_cause") and grounded_hypotheses:
        result["probable_cause"] = grounded_hypotheses[0]["cause"]
    supporting = [item for item in result["evidence"] if item.get("supports_cause")]
    # Only contradictions against the canonical (first) hypothesis weaken the conclusion;
    # evidence that refutes a rejected alternative is what ruling it out looks like.
    contradiction_count = len(grounded_hypotheses[0]["contradicting_evidence"]) if grounded_hypotheses else 0
    return result, supporting, contradiction_count


def _assess_conclusion(
    result: dict[str, Any],
    catalog: dict[str, dict[str, Any]],
    supporting: list[dict[str, Any]],
    contradiction_count: int,
    threshold: float,
) -> tuple[dict[str, Any], list[str], list[str]]:
    reasons = []
    confidence = result.get("confidence")
    if confidence is not None and confidence < threshold:
        reasons.append("confidence below threshold")
    if result.get("probable_cause") and not supporting:
        reasons.append("probable cause has no grounded supporting evidence")

    strength = result.get("conclusion_strength") or "INCONCLUSIVE"
    # Independence is by source: two records from the same source (e.g. log lines and log
    # volume) corroborate each other but are not independent evidence.
    independent_support = {
        catalog[item["evidence_id"]]["source"] for item in supporting if item.get("evidence_id") in catalog
    }
    if strength == "CONFIRMED" and (len(independent_support) < 2 or contradiction_count):
        reasons.append("confirmed conclusion requires two independent supports without contradiction")
        strength = "LIKELY" if supporting and not contradiction_count else "INCONCLUSIVE"
    if strength == "LIKELY" and not supporting:
        reasons.append("likely conclusion requires grounded causal support")
        strength = "INCONCLUSIVE"
    if not supporting and result.get("probable_cause"):
        strength = "INCONCLUSIVE"

    # With no causal claim, confidence rates the health assessment instead, so neither
    # the causal-strength cap nor a re-investigation round applies to it.
    claims_cause = bool(result.get("probable_cause"))
    settled_healthy = not claims_cause and confidence is not None and confidence >= threshold

    evidence_gaps = []
    if strength == "INCONCLUSIVE":
        reasons.append("conclusion inconclusive")
        if not settled_healthy:
            evidence_gaps = [
                check.strip() for check in result.get("next_checks", []) if isinstance(check, str) and check.strip()
            ][:3]
    confidence_out = confidence
    if confidence is not None and claims_cause:
        strength_cap = {"CONFIRMED": 1.0, "LIKELY": 0.84, "INCONCLUSIVE": threshold}[strength]
        confidence_out = min(confidence, strength_cap)
    return (
        {
            **result,
            "conclusion_strength": strength,
            "confidence": confidence_out,
        },
        reasons,
        evidence_gaps,
    )


def route_after_validation(state: RcaAnalysisState) -> str:
    """Re-investigate only when the validator asked for evidence and the request can afford it."""
    if int(state.get("investigation_round", 0)) >= _MAX_INVESTIGATION_ROUNDS:
        return END
    if not state.get("evidence_gaps"):
        return END
    if not state.get("available_sources"):
        return END
    budget = state.get("investigation_budget") or {}
    for key in ("model_calls", "tool_calls"):
        remaining = budget.get(key)
        if remaining is not None and remaining <= 0:
            return END
    if budget.get("expired"):
        return END
    return "plan_evidence"


def _scope_with_discovered_trace(
    scope: IncidentScope,
    merged_evidence: dict[str, Any] | None,
) -> IncidentScope:
    discovered = (merged_evidence or {}).get("discovered_trace_ids") or []
    if scope.trace_id or not discovered:
        return scope
    return scope.model_copy(update={"trace_id": str(discovered[0])})


def _ground_reference(
    reference: dict[str, Any],
    catalog: dict[str, dict[str, str]],
    reasons: list[str],
) -> dict[str, Any] | None:
    evidence_id = reference.get("evidence_id")
    catalog_item = catalog.get(evidence_id)
    if catalog_item is None:
        reasons.append(f"ungrounded evidence_id: {evidence_id}")
        return None
    if reference.get("source") != catalog_item["source"]:
        reasons.append(f"evidence source mismatch: {evidence_id}")
        return None
    return {**reference}


def _ground_hypothesis_reference(
    evidence_id: str,
    catalog: dict[str, dict[str, Any]],
    reasons: list[str],
) -> str | None:
    if evidence_id not in catalog:
        reasons.append(f"ungrounded hypothesis evidence_id: {evidence_id}")
        return None
    return evidence_id


def validate_plan(
    draft: DraftEvidencePlan | dict[str, Any] | None,
    available_sources: list[str],
) -> dict[str, Any]:
    """Keep the planner's hints for sources the agent can actually query.

    Hints are not a branching unit any more: the central agent decides the calls, and the
    toolset already records why a source is not offered.
    """
    if not isinstance(draft, DraftEvidencePlan):
        try:
            draft = DraftEvidencePlan.model_validate(draft or {})
        except Exception:
            draft = DraftEvidencePlan()
    available = set(available_sources)
    hints: list[dict[str, str]] = []
    for task in draft.tasks:
        hint = {"source": task.source, "focus": task.focus}
        if task.source in available and hint not in hints:
            hints.append(hint)
    return {"hints": hints}
