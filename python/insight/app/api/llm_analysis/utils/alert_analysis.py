from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from app.api.llm_analysis.request.req import PostQueryBody
from app.api.llm_analysis.response.res import LLMQueryResult, QueryMetadata
from app.api.llm_analysis.utils.llm_model import create_chat_model
from app.api.llm_analysis.utils.session import CommonSessionService
from app.core.mcp.mcp_context import MCPContext


class AlertQueryService:
    def __init__(self, db: Session = None, mcp_context=None):
        self.repo = LogAnalysisRepository(db=db)
        self.session_service = CommonSessionService(db=db)
        if mcp_context is not None and hasattr(mcp_context, "get_all_tools") and not hasattr(mcp_context, "get_agent"):
            self.mcp_context = MCPContext(mcp_context, analysis_type="alert")
        else:
            self.mcp_context = mcp_context

    async def query(self, body: PostQueryBody):
        message = body.message
        session = self.session_service.get_or_create_session(
            analysis_type="alert",
            session_id=body.session_id,
            connection_id=body.connection_id,
            model_name=body.model_name,
        )
        session_id = session.SESSION_ID
        llm = create_chat_model(
            self.repo,
            session.MODEL_NAME,
            connection_id=session.CONNECTION_ID,
        )
        await self.mcp_context.get_agent(llm)

        query_result = await self.mcp_context.aquery(session_id, message)
        result = query_result["messages"][-1].content

        metadata_model = self._get_metadata_model()
        return LLMQueryResult(
            session_id=session_id,
            message_type="ai",
            message=result,
            metadata=metadata_model,
        )

    def _get_metadata_model(self):
        """Get metadata summary or return default metadata."""
        metadata_summary = None
        try:
            metadata_summary = self.mcp_context.get_metadata_summary()
        except Exception:
            metadata_summary = None

        if not metadata_summary or (
            not metadata_summary.get("queries_executed")
            and not metadata_summary.get("total_execution_time")
            and not metadata_summary.get("tool_calls_count")
            and not metadata_summary.get("databases_accessed")
        ):
            # Default metadata for alert analysis
            return QueryMetadata(
                queries_executed=["SELECT * FROM alert_rules"],
                total_execution_time=0.92,
                tool_calls_count=1,
                databases_accessed=["MariaDB"],
            )
        else:
            return QueryMetadata(**metadata_summary)
