from app.api.llm_analysis.response.res import LLMChatSession, SessionHistory, Message
from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from app.api.llm_analysis.request.req import PostSessionBody, SessionIdPath
from app.core.mcp.mcp_context import MCPContext
from app.core.mcp.multi_mcp_manager import MCPManager
from sqlalchemy.orm import Session
from fastapi import HTTPException, status
import uuid


class CommonSessionService:
    def __init__(self, db: Session = None, mcp_context=None):
        self.repo = LogAnalysisRepository(db=db)
        # mcp_context now receives MCPManager instance
        if isinstance(mcp_context, MCPManager):
            # Wrap MCPManager with MCPContext
            self.mcp_context = MCPContext(mcp_context)
        else:
            self.mcp_context = mcp_context

    def get_sessions(self):
        sessions = self.repo.get_all_sessions()
        results = [self.map_session_to_res(session) for session in sessions]
        return results

    def create_chat_session(self, body: PostSessionBody):
        provider, model_name = body.provider, body.model_name
        session_id = uuid.uuid4()

        session_info = {"USER_ID": 1, "SESSION_ID": session_id, "PROVIDER": provider, "MODEL_NAME": model_name}
        new_session = self.repo.create_session(session_info)

        return self.map_session_to_res(new_session)

    @staticmethod
    def map_session_to_res(session):
        return LLMChatSession(
            seq=session.SEQ, user_id=session.USER_ID, session_id=session.SESSION_ID, provider=session.PROVIDER, model_name=session.MODEL_NAME, regdate=session.REGDATE
        )

    def delete_chat_session(self, path: SessionIdPath):
        session_id = path.sessionId
        session = self.repo.delete_session_by_id(session_id)

        if session:
            deleted_session = self.map_session_to_res(session)
            return deleted_session
        else:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session Not Found")

    def delete_all_chat_sessions(self):
        sessions = self.repo.delete_all_sessions()
        if sessions:
            return [self.map_session_to_res(session) for session in sessions]
        else:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No Sessions Found")

    async def get_chat_session_history(self, path: SessionIdPath):
        session_id = path.sessionId
        session_info = self.repo.get_session_by_id(session_id)

        history = await self.mcp_context.get_chat_history(session_id)
        result = []

        if not session_info:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session Not Found")

        if history:
            channel_values = history.get("channel_values", [])
            for message in channel_values["messages"]:
                if self.filter_message(message):
                    result.append(Message(message_type=message.type, message=message.content))
        return self.map_history_to_res(session_info, result)

    @staticmethod
    def filter_message(element):
        return element.type == "human" or (element.type == "ai" and element.content)

    @staticmethod
    def map_history_to_res(session, messages):
        return SessionHistory(
            seq=session.SEQ,
            user_id=session.USER_ID,
            session_id=session.SESSION_ID,
            provider=session.PROVIDER,
            model_name=session.MODEL_NAME,
            regdate=session.REGDATE,
            messages=messages,
        )