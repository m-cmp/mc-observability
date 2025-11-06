from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from app.api.llm_analysis.request.req import PostQueryBody
from app.api.llm_analysis.response.res import Message
from app.api.llm_analysis.utils.llm_api_key import CredentialService
from app.core.mcp.mcp_context import MCPContext
from app.core.mcp.multi_mcp_manager import MCPManager


class LogQueryService:
    def __init__(self, db: Session = None, mcp_context=None):
        self.repo = LogAnalysisRepository(db=db)
        # mcp_context now receives MCPManager instance
        if isinstance(mcp_context, MCPManager):
            # Wrap MCPManager with MCPContext for log analysis
            self.mcp_context = MCPContext(mcp_context, analysis_type="log")
        else:
            self.mcp_context = mcp_context

    async def query(self, body: PostQueryBody):
        session_id, message = body.session_id, body.message
        session = self.repo.get_session_by_id(session_id)
        if not session:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session Not Found")
        provider_credential = CredentialService(repo=self.repo).get_provider_credential(provider=session.PROVIDER)
        await self.mcp_context.get_agent(session.PROVIDER, session.MODEL_NAME, provider_credential, streaming=False)

        query_result = await self.mcp_context.aquery(session_id, message)
        result = query_result["messages"][-1].content

        return Message(message_type="ai", message=result)

    # async def query_stream(self, body: PostQueryBody):
    #     """Prepare agent and return async generator for SSE streaming."""
    #     session_id, message = body.session_id, body.message
    #     session = self.repo.get_session_by_id(session_id)
    #     if not session:
    #         raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session Not Found")
    #
    #     provider_credential = CredentialService(repo=self.repo).get_provider_credential(provider=session.PROVIDER)
    #     await self.mcp_context.get_agent(session.PROVIDER, session.MODEL_NAME, provider_credential, streaming=True)
    #
    #     # Return async generator for StreamingResponse
    #     return self.mcp_context.astream_query(session_id, message)
