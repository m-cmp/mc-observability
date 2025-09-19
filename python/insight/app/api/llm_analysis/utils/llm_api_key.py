from app.api.llm_analysis.response.res import OpenAIAPIKey, GoogleAPIKey, AnthropicAPIKey
from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from sqlalchemy.orm import Session
from fastapi import HTTPException, status
from typing import Callable
import os


class CredentialService:
    def __init__(self, repo):
        self.repo = repo
        self._fetchers: dict[str, Callable[[], str]] = {
            "openai": self._fetch_openai_key,
            "ollama": self._fetch_ollama_url,
            "google": self._fetch_google_key,
            "anthropic": self._fetch_anthropic_key,
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
        if not url:
            raise HTTPException(detail="OLLAMA_BASE_URL not set", status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)
        return url

    def _fetch_google_key(self) -> str:
        api_key = self.repo.get_google_key()
        if not api_key:
            raise HTTPException(detail="API Key Not Found", status_code=status.HTTP_404_NOT_FOUND)
        return api_key.API_KEY

    def _fetch_anthropic_key(self) -> str:
        api_key = self.repo.get_anthropic_key()
        if not api_key:
            raise HTTPException(detail="API Key Not Found", status_code=status.HTTP_404_NOT_FOUND)
        return api_key.API_KEY


class CommonAPIKeyService:
    def __init__(self, db: Session = None):
        self.repo = LogAnalysisRepository(db=db)

    def get_openai_key(self):
        result = self.repo.get_openai_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key found")
        return OpenAIAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def post_openai_key(self, api_key: str):
        result = self.repo.create_openai_key(api_key)
        return OpenAIAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def delete_openai_key(self):
        result = self.repo.get_openai_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key to delete")
        self.repo.delete_openai_key()
        return OpenAIAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def get_google_key(self):
        result = self.repo.get_google_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key found")
        return GoogleAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def post_google_key(self, api_key: str):
        result = self.repo.create_google_key(api_key)
        return GoogleAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def delete_google_key(self):
        result = self.repo.get_google_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key to delete")
        self.repo.delete_google_key()
        return GoogleAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def get_anthropic_key(self):
        result = self.repo.get_anthropic_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key found")
        return AnthropicAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def post_anthropic_key(self, api_key: str):
        result = self.repo.create_anthropic_key(api_key)
        return AnthropicAPIKey(seq=result.SEQ, api_key=result.API_KEY)

    def delete_anthropic_key(self):
        result = self.repo.get_anthropic_key()
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key to delete")
        self.repo.delete_anthropic_key()
        return AnthropicAPIKey(seq=result.SEQ, api_key=result.API_KEY)


# Legacy compatibility classes
class OpenAIAPIKeyService(CommonAPIKeyService):
    def get_key(self):
        return self.get_openai_key()

    def post_key(self, api_key: str):
        return self.post_openai_key(api_key)

    def delete_key(self):
        return self.delete_openai_key()


class GoogleAPIKeyService(CommonAPIKeyService):
    def get_key(self):
        return self.get_google_key()

    def post_key(self, api_key: str):
        return self.post_google_key(api_key)

    def delete_key(self):
        return self.delete_google_key()


class AnthropicAPIKeyService(CommonAPIKeyService):
    def get_key(self):
        return self.get_anthropic_key()

    def post_key(self, api_key: str):
        return self.post_anthropic_key(api_key)

    def delete_key(self):
        return self.delete_anthropic_key()
