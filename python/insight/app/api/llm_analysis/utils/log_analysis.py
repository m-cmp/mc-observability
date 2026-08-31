from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from app.api.llm_analysis.request.req import PostQueryBody
from app.api.llm_analysis.response.res import LLMQueryResult
from app.api.llm_analysis.utils.llm_model import create_chat_model
from app.api.llm_analysis.utils.session import CommonSessionService
from app.core.mcp.mcp_context import MCPContext


class LogQueryService:
    def __init__(self, db: Session = None, mcp_context=None):
        self.repo = LogAnalysisRepository(db=db)
        self.session_service = CommonSessionService(db=db)
        if mcp_context is not None and hasattr(mcp_context, "get_all_tools") and not hasattr(mcp_context, "get_agent"):
            self.mcp_context = MCPContext(mcp_context, analysis_type="log")
        else:
            self.mcp_context = mcp_context

    async def query(self, body: PostQueryBody):
        message = body.message
        session = self.session_service.get_or_create_session(
            analysis_type="log",
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

        return LLMQueryResult(
            session_id=session_id,
            message_type="ai",
            message=result,
        )
