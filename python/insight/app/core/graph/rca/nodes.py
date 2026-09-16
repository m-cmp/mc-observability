import json
import logging
from typing import Any

from langgraph.graph import END, START, StateGraph
from langgraph.runtime import Runtime

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
    PlannedHypothesis,
    RcaAnalysisState,
    RcaResult,
    RcaRunContext,
)
from .prompts import PLAN_SYSTEM_PROMPT, SYNTHESIS_SYSTEM_PROMPT
from .specs import SOURCE_SPECS

logger = logging.getLogger(__name__)

_DEFAULT_QUERY = "Analyze the incident and identify the most probable evidence-backed cause."
_MAX_INVESTIGATION_ROUNDS = 1
_MAX_HYPOTHESES = 3
# A source the agent chose not to query is not a gap; a source that could not be offered is.
_NOT_QUERIED = "not_queried"


class RcaGraphNodes:
    async def plan_evidence(
        self,
        state: RcaAnalysisState,
        runtime: Runtime[RcaRunContext],
    ) -> dict[str, Any]:
        available = runtime.context.investigation_toolset.queryable_sources
        draft = DraftEvidencePlan()
        try:
            draft = DraftEvidencePlan.model_validate(
                await runtime.context.llm.with_structured_output(DraftEvidencePlan).ainvoke(
                    [
                        {"role": "system", "content": PLAN_SYSTEM_PROMPT},
                        {
                            "role": "user",
                            "content": json.dumps(
                                {
                                    "query": state.get("query"),
                                    "sources": {source: SOURCE_SPECS[source]["summary"] for source in available},
                                    "scope": state.get("scope") or {},
                                    "filters": state.get("filters") or {},
                                },
                                ensure_ascii=False,
                                default=str,
                            ),
                        },
                    ]
                )
            )
        except Exception as exc:
            # The agent can still investigate without a plan; the log says why it had none.
            logger.warning("RCA plan drafting failed: %s", exc)
        return {"query": state.get("query") or _DEFAULT_QUERY, "plan": validate_plan(draft, available)}

    async def investigate_evidence(
        self,
        state: RcaAnalysisState,
        runtime: Runtime[RcaRunContext],
    ) -> dict[str, Any]:
        """One central agent round, then the store projection that the graph works from."""
        context = runtime.context
        toolset = context.investigation_toolset
        config = context.analysis_config
        plan = state.get("plan") or DraftEvidencePlan()
        retry_task = state.get("retry_task")
        round_number = 1 if retry_task else 0
        limitations: list[dict[str, Any]] = []
        blocked_reason: str | None = None

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
                    query=state.get("query"),
                    scope=scope,
                    filters=state.get("filters"),
                    # The retry round's plan is its retry_task.
                    plan=[] if retry_task else [item.model_dump(mode="json", exclude_none=True) for item in plan.hypotheses],
                    prior_evidence_catalog=list(previous.get("evidence_catalog") or []),
                    prior_tool_calls=toolset.ledger(),
                    retry_task=retry_task,
                    investigation_round=round_number,
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
                    blocked_reason = exc.code
                else:
                    try:
                        await runner.ainvoke({"messages": [{"role": "user", "content": fitted["text"]}]})
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
        return {"merged_evidence": merged, "investigation_round": round_number, "retried_task": retry_task}

    async def synthesize(
        self,
        state: RcaAnalysisState,
        runtime: Runtime[RcaRunContext],
    ) -> dict[str, Any]:
        # The evidence store caps the catalog at admission to a share of the context window,
        # so synthesis does not trim it again.
        merged = state.get("merged_evidence") or {}
        if reason := merged.get("synthesis_blocked_reason"):
            return {"analysis_result": None, "error_message": reason}
        if not merged.get("evidence_catalog"):
            return {"analysis_result": None, "error_message": "No usable evidence was collected."}
        last_error = "synthesis failed"
        for _ in range(2):
            try:
                result = await runtime.context.llm.with_structured_output(RcaResult).ainvoke(
                    [
                        {"role": "system", "content": SYNTHESIS_SYSTEM_PROMPT},
                        {
                            "role": "user",
                            "content": json.dumps(
                                {
                                    "query": state.get("query"),
                                    "scope": state.get("scope"),
                                    "candidate_hypotheses": [
                                        item.statement for item in (state.get("plan") or DraftEvidencePlan()).hypotheses
                                    ],
                                    "investigation_round": state.get("investigation_round", 0),
                                    "merged_evidence": {
                                        key: merged[key] for key in ("evidence_catalog", "sources", "limitations")
                                    },
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
                return {
                    "analysis_result": result.model_dump(mode="json"),
                    "retry_task": result.retry_task.model_dump(mode="json") if result.retry_task else None,
                }
            except Exception as exc:
                last_error = str(exc)
        return {"analysis_result": None, "error_message": last_error}

    async def validate_result(
        self,
        state: RcaAnalysisState,
        runtime: Runtime[RcaRunContext],
    ) -> dict[str, Any]:
        return _validate_result(
            state.get("analysis_result"),
            state.get("merged_evidence"),
            runtime.context.analysis_config,
            state.get("retry_task"),
        )


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
    }


def build_rca_graph():
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
        ["investigate_evidence", END],
    )
    return graph.compile()


def _validate_result(
    result: dict | None,
    merged_evidence: dict | None,
    analysis_config: dict,
    retry_task: dict[str, str] | None = None,
) -> dict[str, Any]:
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
    execution_reasons = _execution_reasons(result, sources, limitations)
    # An empty catalog because every queried source ran and found nothing is a finding, not
    # a broken analysis. Only a source that actually failed makes the run unusable.
    no_telemetry = all(status in {"NO_DATA", "SKIPPED"} for status in sources.values()) and "NO_DATA" in sources.values()
    if not catalog:
        execution_reasons.append("no telemetry data in the requested window" if no_telemetry else "no usable evidence")
        result = None
    conclusion_reasons: list[str] = []
    can_retry = False
    if isinstance(result, dict):
        result, supporting, contradiction_count = _ground_result_references(
            result,
            catalog,
            execution_reasons,
        )
        result, conclusion_reasons, can_retry = _assess_conclusion(
            result,
            catalog,
            supporting,
            contradiction_count,
            threshold,
        )

    execution_reasons = list(dict.fromkeys(execution_reasons))
    conclusion_reasons = list(dict.fromkeys(conclusion_reasons))
    if result is None:
        status = "PARTIAL" if no_telemetry else "FAILED"
    else:
        status = "PARTIAL" if execution_reasons else "SUCCEEDED"
    return {
        "analysis_result": result,
        "retry_task": retry_task if can_retry else None,
        "result_validation": {
            "status": status,
            "no_telemetry": no_telemetry,
            "reasons": execution_reasons,
            "conclusion_reasons": conclusion_reasons,
            "usable_evidence_count": len(catalog),
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
    if result.get("probable_cause"):
        if grounded_hypotheses:
            result["probable_cause"] = grounded_hypotheses[0]["cause"]
        support_ids = dict.fromkeys(grounded_hypotheses[0]["supporting_evidence"] if grounded_hypotheses else [])
        supporting = [catalog[evidence_id] for evidence_id in support_ids]
        # Keep the public flag consistent with the selected cause's authoritative links.
        for item in result["evidence"]:
            item["supports_cause"] = item["evidence_id"] in support_ids
    else:
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
) -> tuple[dict[str, Any], list[str], bool]:
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

    # With no causal claim, confidence rates the health assessment instead, so neither
    # the causal-strength cap nor a re-investigation round applies to it.
    claims_cause = bool(result.get("probable_cause"))
    settled_healthy = not claims_cause and confidence is not None and confidence >= threshold

    if strength == "INCONCLUSIVE":
        reasons.append("conclusion inconclusive")
    confidence_out = confidence
    if confidence is not None and claims_cause:
        strength_cap = {"CONFIRMED": 1.0, "LIKELY": 0.84, "INCONCLUSIVE": threshold}[strength]
        confidence_out = min(confidence, strength_cap)
    return (
        {
            **result,
            "conclusion_strength": strength,
            "confidence": confidence_out,
            "probable_cause": result.get("probable_cause") if supporting else "",
        },
        reasons,
        strength == "INCONCLUSIVE" and not settled_healthy,
    )


def route_after_validation(state: RcaAnalysisState, runtime: Runtime[RcaRunContext]) -> str:
    """Run one focused follow-up only when its source and budget are available."""
    if int(state.get("investigation_round", 0)) >= _MAX_INVESTIGATION_ROUNDS:
        return END
    task = state.get("retry_task")
    toolset = runtime.context.investigation_toolset
    if not task or not toolset or task.get("source") not in toolset.queryable_sources or not task.get("focus", "").strip():
        return END
    budget = runtime.context.budget
    if budget is not None and (budget.expired or not budget.remaining_model_calls or not budget.remaining_tool_calls):
        return END
    return "investigate_evidence"


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
    return {**reference, "observation": catalog_item["observation"]}


def _ground_hypothesis_reference(
    evidence_id: str,
    catalog: dict[str, dict[str, Any]],
    reasons: list[str],
) -> str | None:
    if evidence_id not in catalog:
        reasons.append(f"ungrounded hypothesis evidence_id: {evidence_id}")
        return None
    return evidence_id


def validate_plan(draft: DraftEvidencePlan, available_sources: list[str]) -> DraftEvidencePlan:
    """Keep up to three distinct hypotheses; drop the checks this request cannot run."""
    kept: dict[str, PlannedHypothesis] = {}
    for hypothesis in draft.hypotheses:
        statement = hypothesis.statement.strip()
        if not statement or statement in kept:
            continue
        check = hypothesis.check
        feasible = check is not None and check.source in available_sources and bool(check.focus.strip())
        kept[statement] = PlannedHypothesis(statement=statement, check=check if feasible else None)
    return DraftEvidencePlan(hypotheses=list(kept.values())[:_MAX_HYPOTHESES])
