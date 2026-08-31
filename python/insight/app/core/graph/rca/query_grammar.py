"""Whitelist validators for the LogQL and TraceQL the investigation agent writes.

The agent composes the query text; code decides what shapes may reach the backend. Both
validators are small recursive-descent parsers over the *allowed* grammar and reject
anything they cannot derive — they are not a list of forbidden tokens.

Allowed LogQL:   selector ( line_filter )*
    selector     := "{" matcher ( "," matcher )* "}"
    matcher      := ident ( "=" | "!=" | "=~" | "!~" ) string
    line_filter  := ( "|=" | "!=" | "|~" | "!~" ) string

Allowed TraceQL: "{" [ expr ] "}"
    expr         := term ( ( "&&" | "||" ) term )*
    term         := "(" expr ")" | field op value | "true" | "false"
    field        := [.]ident(.ident|:ident)*
    value        := string | number[unit] | bare identifier
"""

import re

_WS = re.compile(r"\s*")
_IDENT = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")
_TRACE_FIELD = re.compile(r"\.?[A-Za-z_][A-Za-z0-9_]*(?:[.:][A-Za-z_][A-Za-z0-9_]*)*")
_NUMBER = re.compile(r"-?\d+(?:\.\d+)?(?:ns|us|µs|ms|s|m|h)?")
_LOGQL_MATCHER_OPS = ("=~", "!~", "!=", "=")
_LOGQL_FILTER_OPS = ("|=", "!=", "|~", "!~")
_TRACEQL_OPS = ("=~", "!~", "!=", ">=", "<=", "=", ">", "<")
# Bare identifiers Tempo understands without a scope (TraceQL intrinsics, verified against
# Tempo 2.9.2's /api/search); anything else must be scoped. Scoped intrinsics such as span:id,
# trace:rootName, event:name, link:traceID and instrumentation:name pass by prefix below.
_TRACEQL_INTRINSICS = {
    "status",
    "statusMessage",
    "duration",
    "name",
    "kind",
    "rootName",
    "rootServiceName",
    "traceDuration",
    "nestedSetLeft",
    "nestedSetRight",
    "nestedSetParent",
}
_TRACEQL_SCOPES = ("span.", "resource.", "event.", "link.", "instrumentation.", "parent.", ".")
_TRACEQL_SCOPED_INTRINSICS = ("span:", "trace:", "event:", "link:", "instrumentation:", "parent:")

_LOGQL_SHAPE = 'LogQL must be a stream selector {label="value", ...} followed only by line filters (|=, !=, |~, !~)'
_TRACEQL_SHAPE = "TraceQL must be a single spanset filter { ... } of comparisons joined by && or ||"


def validate_logql(query: str, *, allow_line_filters: bool = True) -> str | None:
    """Return None when ``query`` is selection-only LogQL, else the reason it is not.

    ``allow_line_filters=False`` restricts the query to the bare stream selector, which is
    all Loki's index-stats API accepts.
    """
    text = query.strip()
    if not text:
        return f"empty query; {_LOGQL_SHAPE}"
    if text[0] != "{":
        return f"{_LOGQL_SHAPE}; the query does not start with a selector"
    pos, matcher_count, error = _parse_matchers(text, 1)
    if error:
        return error
    if matcher_count == 0:
        return "the stream selector needs at least one label matcher; {} alone is not allowed"
    while True:
        pos = _skip(text, pos)
        if pos == len(text):
            return None
        if not allow_line_filters:
            return "this query takes a stream selector only ({label=\"value\", ...}); drop the line filters"
        op = _take(text, pos, _LOGQL_FILTER_OPS)
        if op is None:
            return f"{_LOGQL_SHAPE}; found {text[pos:pos + 12]!r} after the selector"
        pos, error = _parse_string(text, _skip(text, pos + len(op)), "line filter")
        if error:
            return error


def validate_traceql(query: str) -> str | None:
    """Return None when ``query`` is a single selection-only spanset, else the reason."""
    text = query.strip()
    if not text:
        return f"empty query; {_TRACEQL_SHAPE}"
    if text[0] != "{":
        return f"{_TRACEQL_SHAPE}; the query does not start with a spanset"
    pos = _skip(text, 1)
    if pos < len(text) and text[pos] == "}":
        return _only_whitespace_follows(text, pos + 1)
    pos, error = _parse_trace_expr(text, pos)
    if error:
        return error
    pos = _skip(text, pos)
    if pos >= len(text) or text[pos] != "}":
        return f"unterminated spanset; expected '}}' near {text[pos:pos + 12]!r}"
    return _only_whitespace_follows(text, pos + 1)


# --- LogQL ---------------------------------------------------------------------------


def _parse_matchers(text: str, pos: int) -> tuple[int, int, str | None]:
    count = 0
    while True:
        pos = _skip(text, pos)
        if pos < len(text) and text[pos] == "}" and count == 0:
            return pos + 1, 0, None
        match = _IDENT.match(text, pos)
        if not match:
            return pos, count, f"expected a label name in the stream selector near {text[pos:pos + 12]!r}"
        pos = _skip(text, match.end())
        op = _take(text, pos, _LOGQL_MATCHER_OPS)
        if op is None:
            return pos, count, f"expected =, !=, =~ or !~ after label {match.group()!r}"
        pos, error = _parse_string(text, _skip(text, pos + len(op)), "label matcher")
        if error:
            return pos, count, error
        count += 1
        pos = _skip(text, pos)
        if pos >= len(text):
            return pos, count, "unterminated stream selector; expected '}'"
        if text[pos] == ",":
            pos += 1
            continue
        if text[pos] == "}":
            return pos + 1, count, None
        return pos, count, f"unexpected {text[pos:pos + 12]!r} inside the stream selector"


# --- TraceQL -------------------------------------------------------------------------


def _parse_trace_expr(text: str, pos: int) -> tuple[int, str | None]:
    pos, error = _parse_trace_term(text, pos)
    if error:
        return pos, error
    while True:
        pos = _skip(text, pos)
        op = _take(text, pos, ("&&", "||"))
        if op is None:
            return pos, None
        pos, error = _parse_trace_term(text, _skip(text, pos + 2))
        if error:
            return pos, error


def _parse_trace_term(text: str, pos: int) -> tuple[int, str | None]:
    pos = _skip(text, pos)
    if pos >= len(text):
        return pos, "unterminated spanset; expected a comparison"
    if text[pos] == "(":
        pos, error = _parse_trace_expr(text, pos + 1)
        if error:
            return pos, error
        pos = _skip(text, pos)
        if pos >= len(text) or text[pos] != ")":
            return pos, "unbalanced parenthesis inside the spanset"
        return pos + 1, None
    match = _TRACE_FIELD.match(text, pos)
    if not match:
        return pos, f"{_TRACEQL_SHAPE}; found {text[pos:pos + 12]!r}"
    field = match.group()
    pos = _skip(text, match.end())
    if pos < len(text) and text[pos] == "(":
        return pos, f"functions and aggregates such as {field}() are not allowed inside the spanset"
    if field in ("true", "false"):
        return pos, None
    if not _traceql_field_is_scoped(field):
        return pos, (
            f"attribute {field!r} needs a scope: span.{field} for span attributes, resource.{field} for "
            "resource attributes (or a leading dot to search both); only intrinsics such as status, "
            "duration, name and kind are bare"
        )
    op = _take(text, pos, _TRACEQL_OPS)
    if op is None:
        return pos, f"expected a comparison operator after {field!r}"
    pos = _skip(text, pos + len(op))
    return _parse_trace_value(text, pos)


def _parse_trace_value(text: str, pos: int) -> tuple[int, str | None]:
    if pos < len(text) and text[pos] in ('"', "`"):
        return _parse_string(text, pos, "comparison")
    match = _NUMBER.match(text, pos)
    if match and not _TRACE_FIELD.match(text, pos):
        return match.end(), None
    match = _TRACE_FIELD.match(text, pos)
    if match:
        end = _skip(text, match.end())
        if end < len(text) and text[end] == "(":
            return pos, f"functions and aggregates such as {match.group()}() are not allowed inside the spanset"
        return match.end(), None
    return pos, f"expected a value near {text[pos:pos + 12]!r}"


def _traceql_field_is_scoped(field: str) -> bool:
    return field in _TRACEQL_INTRINSICS or field.startswith(_TRACEQL_SCOPES) or field.startswith(_TRACEQL_SCOPED_INTRINSICS)


# --- shared --------------------------------------------------------------------------


def _parse_string(text: str, pos: int, where: str) -> tuple[int, str | None]:
    if pos >= len(text) or text[pos] not in ('"', "`"):
        return pos, f"expected a quoted string in the {where} near {text[pos:pos + 12]!r}"
    quote = text[pos]
    index = pos + 1
    while index < len(text):
        char = text[index]
        if quote == '"' and char == "\\":
            index += 2
            continue
        if char == quote:
            return index + 1, None
        index += 1
    return index, f"unterminated string in the {where}"


def _only_whitespace_follows(text: str, pos: int) -> str | None:
    pos = _skip(text, pos)
    if pos == len(text):
        return None
    return (
        f"{_TRACEQL_SHAPE}; pipelines (|), aggregates and structural operators between spansets "
        f"are not allowed — found {text[pos:pos + 12]!r} after the spanset"
    )


def _skip(text: str, pos: int) -> int:
    return _WS.match(text, pos).end()


def _take(text: str, pos: int, candidates: tuple[str, ...]) -> str | None:
    for candidate in candidates:
        if text.startswith(candidate, pos):
            return candidate
    return None
