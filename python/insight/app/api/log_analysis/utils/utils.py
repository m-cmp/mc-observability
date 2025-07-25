from app.api.log_analysis.response.res import LogAnalysisModel, LogAnalysisSession, SessionHistory, Message, OpenAIAPIKey, GoogleAPIKey
from app.api.log_analysis.repo.repo import LogAnalysisRepository
from app.api.log_analysis.request.req import PostSessionBody, SessionIdPath, PostQueryBody
from app.core.mcp.mcp_context import MCPContext
from sqlalchemy.orm import Session
from fastapi import HTTPException, status
from typing import Callable
import os
import uuid


class LogAnalysisService:
    PROVIDER_ENV_MAP = {"ollama": "OLLAMA_BASE_URL", "openai": "OPENAI_API_KEY", "google": "GOOGLE_API_KEY"}

    def __init__(self, db: Session = None, mcp_context: MCPContext = None):
        self.repo = LogAnalysisRepository(db=db)
        self.mcp_context = mcp_context

    def get_model_list(self, model_info_config):
        result = []
        for model_info in model_info_config:
            env_key = self.PROVIDER_ENV_MAP[model_info["provider"]]
            if model_info["provider"] == "ollama" and os.getenv(env_key):
                result.append(self.map_model_to_res(model_info))
            elif model_info["provider"] == "openai" and self.repo.get_openai_key():
                result.append(self.map_model_to_res(model_info))
            elif model_info["provider"] == "google" and self.repo.get_google_key():
                result.append(self.map_model_to_res(model_info))
            else:
                pass
        return result

    @staticmethod
    def map_model_to_res(model_info):
        return LogAnalysisModel(provider=model_info["provider"], model_name=model_info["model_name"])

    def get_sessions(self):
        sessions = self.repo.get_all_sessions()
        results = [self.map_session_to_res(session) for session in sessions]
        return results

    def create_chat_session(self, body: PostSessionBody):
        provider, model_name = body.provider, body.model_name
        if provider not in self.PROVIDER_ENV_MAP:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST, detail=f"Invalid provider: {provider}. Must be one of {list(self.PROVIDER_ENV_MAP.keys())}"
            )
        session_id = uuid.uuid4()

        session_info = {"USER_ID": 1, "SESSION_ID": session_id, "PROVIDER": provider, "MODEL_NAME": model_name}
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
        self.repo.delete_all_sessions()
        return []

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

    async def query(self, body: PostQueryBody):
        session_id, message = body.session_id, body.message
        session = self.repo.get_session_by_id(session_id)
        if not session:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session Not Found")
        provider_credential = CredentialService(repo=self.repo).get_provider_credential(provider=session.PROVIDER)
        await self.mcp_context.get_agent(session.PROVIDER, session.MODEL_NAME, provider_credential)

        query_result = await self.mcp_context.aquery(session_id, message)
        result = query_result["messages"][-1].content

        return Message(message_type="ai", message=result)


class CredentialService:
    def __init__(self, repo):
        self.repo = repo
        self._fetchers: dict[str, Callable[[], str]] = {
            "openai": self._fetch_openai_key,
            "ollama": self._fetch_ollama_url,
            "google": self._fetch_google_key,
        }

    def get_provider_credential(self, provider: str) -> str:
        return self._fetchers[provider]()

    def _fetch_openai_key(self) -> str:
        api_key = self.repo.get_openai_key()
        if not api_key:
            raise HTTPException(detail="API Key Not Found", status_code=status.HTTP_404_NOT_FOUND)
        return api_key.API_KEY

    @staticmethod
    def _fetch_ollama_url() -> str:
        url = os.getenv("OLLAMA_BASE_URL")
        print(url)
        if not url:
            raise HTTPException(detail="OLLAMA_BASE_URL not set", status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)
        return url

    def _fetch_google_key(self) -> str:
        api_key = self.repo.get_google_key()
        if not api_key:
            raise HTTPException(detail="GOOGLE_API_KEY not set", status_code=status.HTTP_404_NOT_FOUND)
        return api_key


class OpenAIAPIKeyService:
    def __init__(self, db: Session = None):
        self.repo = LogAnalysisRepository(db=db)

    def get_key(self):
        result = self.repo.get_openai_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key found")
        return OpenAIAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def post_key(self, api_key: str):
        result = self.repo.create_openai_key(api_key)
        return OpenAIAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def delete_key(self):
        result = self.repo.get_openai_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key to delete")
        self.repo.delete_openai_key()
        return OpenAIAPIKey(seq=result.SEQ, api_key=result.API_KEY)


class GoogleAPIKeyService:
    def __init__(self, db: Session = None):
        self.repo = LogAnalysisRepository(db=db)

    def post_key(self, api_key: str):
        result = self.repo.create_google_key(api_key)
        return GoogleAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def delete_key(self):
        result = self.repo.get_google_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key to delete")
        self.repo.delete_google_key()
        return GoogleAPIKey(seq=result.SEQ, api_key=result.API_KEY)
