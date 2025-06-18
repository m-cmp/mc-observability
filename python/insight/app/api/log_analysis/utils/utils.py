from config.ConfigManager import ConfigManager
from app.api.log_analysis.response.res import LogAnalysisModel, LogAnalysisSession, SessionHistory, Message
from app.api.log_analysis.repo.repo import LogAnalysisRepository
from app.api.log_analysis.request.req import PostSessionBody, SessionIdPath, PostQueryBody
from app.core.mcp.mcp_context import MCPContext

from sqlalchemy.orm import Session

from fastapi.responses import JSONResponse
from fastapi import HTTPException, status

import os
import uuid


class LogAnalysisService:
    PROVIDER_ENV_MAP = {
        'ollama': 'OLLAMA_BASE_URL',
        'openai': 'OPENAI_API_KEY'
    }

    def __init__(self, db: Session=None, mcp_context: MCPContext=None):
        self.repo = LogAnalysisRepository(db=db)
        self.mcp_context = mcp_context

    def get_model_list(self, model_info_config):
        result = []
        for model_info in model_info_config:
            env_key = self.PROVIDER_ENV_MAP[model_info['provider']]
            if env_key and os.getenv(env_key):
                result.append(self.map_model_to_res(model_info))
        return result

    @staticmethod
    def map_model_to_res(model_info):
        return LogAnalysisModel(
            provider=model_info['provider'],
            model_name=model_info['model_name']
        )

    def get_sessions(self):
        sessions = self.repo.get_all_sessions()
        results = [
            self.map_session_to_res(session) for session in sessions
        ]
        return results

    def create_chat_session(self, body: PostSessionBody):
        provider, model_name = body.provider, body.model_name
        session_id = uuid.uuid4()

        session_info = {
            'USER_ID': 1,
            'SESSION_ID': session_id,
            'PROVIDER': provider,
            'MODEL_NAME': model_name
        }
        new_session = self.repo.create_session(session_info)

        return self.map_session_to_res(new_session)

    @staticmethod
    def map_session_to_res(session):
        return LogAnalysisSession(
            seq=session.SEQ,
            user_id=session.USER_ID,
            session_id=session.SESSION_ID,
            provider=session.PROVIDER,
            model_name=session.MODEL_NAME,
            regdate=session.REGDATE
        )

    def delete_chat_session(self, path: SessionIdPath):
        session_id = path.sessionId
        session = self.repo.delete_session_by_id(session_id)

        if session:
            deleted_session = self.map_session_to_res(session)
            return deleted_session
        else:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Session Not Found"
            )


    async def get_chat_session_history(self, path: SessionIdPath):
        session_id = path.sessionId
        session_info = self.repo.get_session_by_id(session_id)

        history = await self.mcp_context.get_chat_history(session_id)
        result = []
        
        if not session_info:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Session Not Found"
            )

        if history:
            channel_values = history.get('channel_values', [])
            for message in channel_values['messages']:
                if self.filter_message(message):
                    result.append(
                        Message(
                            message_type=message.type,
                            message=message.content
                        )
                    )
        return self.map_history_to_res(session_info, result)


    @staticmethod
    def filter_message(element):
        return (
            element.type == 'human' or
            (element.type == 'ai' and element.content)
        )

    @staticmethod
    def map_history_to_res(session, messages):
        return SessionHistory(
            seq=session.SEQ,
            user_id=session.USER_ID,
            session_id=session.SESSION_ID,
            provider=session.PROVIDER,
            model_name=session.MODEL_NAME,
            regdate=session.REGDATE,
            messages=messages
        )

    async def query(self, body: PostQueryBody):
        session_id, message = body.session_id, body.message
        session = self.repo.get_session_by_id(session_id)
        await self.mcp_context.get_agent(session.PROVIDER, session.MODEL_NAME)

        query_result = await self.mcp_context.aquery(session_id, message)
        result = query_result['messages'][-1].content

        return Message(
            message_type='ai',
            message=result
        )