import uuid

from fastapi import HTTPException, status
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver
from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from app.api.llm_analysis.request.req import PostSessionBody, SessionIdPath
from app.api.llm_analysis.response.res import LLMChatSession, Message, SessionHistory


class CommonSessionService:
    def __init__(self, db: Session = None):
        self.repo = LogAnalysisRepository(db=db)

    def get_sessions(self):
        sessions = self.repo.get_all_sessions()
        results = [self.map_session_to_res(session) for session in sessions]
        return results

    def create_chat_session(self, body: PostSessionBody):
        connection = self.resolve_connection(body.connection_id)
        model_name = body.model_name or connection.DEFAULT_MODEL
        if not model_name:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="No model is configured for the selected LLM connection",
            )
        session_id = str(uuid.uuid4())

        session_info = {
            "USER_ID": 1,
            "SESSION_ID": session_id,
            "CONNECTION_ID": connection.SEQ,
            "ANALYSIS_TYPE": body.analysis_type,
            "PROVIDER": connection.PROVIDER,
            "MODEL_NAME": model_name,
        }
        new_session = self.repo.create_session(session_info)

        return self.map_session_to_res(new_session)

    def resolve_connection(self, connection_id=None):
        if connection_id is not None:
            connection = self.repo.get_connection_by_id(connection_id)
            if not connection:
                raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="LLM connection not found")
            if not connection.ENABLED:
                raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="LLM connection is disabled")
            return connection
        connection = self.repo.get_default_connection()
        if not connection:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Default LLM connection is not configured",
            )
        return connection

    def get_or_create_session(
        self,
        *,
        analysis_type: str,
        session_id: str | None,
        connection_id: int | None = None,
        model_name: str | None = None,
    ):
        if session_id:
            session = self.repo.get_session_by_id(session_id)
            if not session:
                raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session Not Found")
            if session.CONNECTION_ID is None:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="Session is not associated with an LLM connection",
                )
            if session.ANALYSIS_TYPE != analysis_type:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail=f"Session belongs to {session.ANALYSIS_TYPE} analysis",
                )
            return session
        created = self.create_chat_session(
            PostSessionBody(
                analysis_type=analysis_type,
                connection_id=connection_id,
                model_name=model_name,
            )
        )
        return self.repo.get_session_by_id(created.session_id)

    def map_session_to_res(self, session):
        connection = self.repo.get_connection_by_id(session.CONNECTION_ID) if session.CONNECTION_ID else None
        return LLMChatSession(
            seq=session.SEQ,
            user_id=session.USER_ID,
            session_id=session.SESSION_ID,
            connection_id=session.CONNECTION_ID,
            connection_name=connection.NAME if connection else None,
            analysis_type=session.ANALYSIS_TYPE,
            provider=session.PROVIDER,
            model_name=session.MODEL_NAME,
            regdate=session.REGDATE,
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
        if not session_info:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session Not Found")

        config = {"configurable": {"thread_id": session_id, "checkpoint_ns": ""}}
        async with AsyncSqliteSaver.from_conn_string(
            "checkpoints/checkpoints.sqlite"
        ) as memory:
            history = await memory.aget(config)
        result = []
        if history:
            channel_values = history.get("channel_values") or {}
            for message in channel_values.get("messages", []):
                if self.filter_message(message):
                    result.append(Message(message_type=message.type, message=message.content))
        return self.map_history_to_res(session_info, result)

    @staticmethod
    def filter_message(element):
        return element.type == "human" or (element.type == "ai" and element.content)

    def map_history_to_res(self, session, messages):
        connection = self.repo.get_connection_by_id(session.CONNECTION_ID) if session.CONNECTION_ID else None
        return SessionHistory(
            seq=session.SEQ,
            user_id=session.USER_ID,
            session_id=session.SESSION_ID,
            connection_id=session.CONNECTION_ID,
            connection_name=connection.NAME if connection else None,
            analysis_type=session.ANALYSIS_TYPE,
            provider=session.PROVIDER,
            model_name=session.MODEL_NAME,
            regdate=session.REGDATE,
            messages=messages,
        )
