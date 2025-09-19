from app.api.llm_analysis.response.res import LLMAPIKey
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
        return self._fetchers[provider](provider)

    def _fetch_openai_key(self, provider) -> str:
        api_key = self.repo.get_api_key(provider)
        if not api_key:
            raise HTTPException(detail="API Key Not Found", status_code=status.HTTP_404_NOT_FOUND)
        return api_key.API_KEY

    @staticmethod
    def _fetch_ollama_url(provider) -> str:
        url = os.getenv("OLLAMA_BASE_URL")
        if not url:
            raise HTTPException(detail="OLLAMA_BASE_URL not set", status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)
        return url

    def _fetch_google_key(self, provider) -> str:
        api_key = self.repo.get_api_key(provider)
        if not api_key:
            raise HTTPException(detail="API Key Not Found", status_code=status.HTTP_404_NOT_FOUND)
        return api_key.API_KEY

    def _fetch_anthropic_key(self, provider) -> str:
        api_key = self.repo.get_api_key(provider)
        if not api_key:
            raise HTTPException(detail="API Key Not Found", status_code=status.HTTP_404_NOT_FOUND)
        return api_key.API_KEY


class CommonAPIKeyService:
    def __init__(self, db: Session = None):
        self.repo = LogAnalysisRepository(db=db)

    @staticmethod
    def map_key_to_res(key):
        return LLMAPIKey(seq=key.SEQ, provider=key.PROVIDER, api_key=key.API_KEY)

    def get_api_key(self, provider: str = None):
        keys = self.repo.get_api_key(provider=provider)
        if not keys:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key found")

        if not provider:
            return [self.map_key_to_res(key) for key in keys]
        else:
            return [self.map_key_to_res(keys)]


    def post_api_key(self, provider: str, api_key: str):
        result = self.repo.post_api_key(provider=provider, api_key=api_key)
        return self.map_key_to_res(result)

    def delete_api_key(self, provider: str):
        result = self.repo.delete_api_key(provider=provider)
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key found")
        return self.map_key_to_res(result)
