from config.ConfigManager import ConfigManager
from app.api.log_analysis.response.res import LogAnalysisModel, LogAnalysisSession, SessionHistory
from app.api.log_analysis.repo.repo import LogAnalysisRepository
from app.api.log_analysis.request.req import PostSessionBody, GetHistoryPath
from app.core.mcp.mcp_context import MCPContext

from sqlalchemy.orm import Session

import os
import uuid


class LogAnalysisService:
    def __init__(self, db: Session=None, mcp_context: MCPContext=None):
        self.repo = LogAnalysisRepository(db=db)
        self.mcp_context = mcp_context

    def get_model_list(self, model_info_config):
        result = []
        for model_info in model_info_config:
            if model_info['provider'] == 'ollama':
                ollama_base_url = os.getenv('OLLAMA_BASE_URL', None)
                if ollama_base_url:
                    result.append(self.map_model_to_res(model_info))

            elif model_info['provider'] == 'openai':
                openai_api_key = os.getenv('OPENAI_API_KEY', None)
                if openai_api_key:
                    result.append(self.map_model_to_res(model_info))

        return result

    @staticmethod
    def map_model_to_res(model_info):
        return LogAnalysisModel(
            provider=model_info['provider'],
            model_name=model_info['model_name']
        )

    def get_chat_session(self):
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

    async def get_chat_session_history(self, path: GetHistoryPath):
        session_id = path.sessionId
        session_id = 'dbe2cb8c-1d91-4361-80ed-9df8623cb857'
        history = await self.mcp_context.get_chat_history(session_id)

        channel_values = history['channel_values']

        result = []
        for message in channel_values['messages']:
            if self.filter_message(message):
                result.append(
                    SessionHistory(
                        message_type=message.type,
                        message=message.content
                    )
                )
        return result


    @staticmethod
    def filter_message(element):
        return (
            element.type == 'human' or
            (element.type == 'ai' and element.content)
        )