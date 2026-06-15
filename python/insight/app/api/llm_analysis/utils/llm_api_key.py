import os
from dataclasses import dataclass

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from app.api.llm_analysis.response.res import LLMAPIKey


@dataclass(frozen=True)
class LLMEndpointConfig:
    provider: str
    api_key: str | None = None
    base_url: str | None = None


class CredentialService:
    def __init__(self, repo):
        self.repo = repo

    @staticmethod
    def _provider_value(provider) -> str:
        return getattr(provider, "value", provider)

    def get_provider_config(self, provider: str) -> LLMEndpointConfig:
        provider_value = self._provider_value(provider)
        if provider_value == "ollama":
            return self._get_ollama_config(provider_value)
        if provider_value == "openai-compatible":
            return self._get_openai_compatible_config(provider_value)
        if provider_value in ("openai", "google", "anthropic"):
            return self._get_api_key_config(provider_value)
        raise HTTPException(
            detail=f"Unsupported provider: {provider_value}",
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def _get_api_key_config(self, provider: str) -> LLMEndpointConfig:
        api_key_record = self.repo.get_api_key(provider)
        if not api_key_record or not api_key_record.API_KEY:
            raise HTTPException(detail="API Key Not Found", status_code=status.HTTP_404_NOT_FOUND)
        return LLMEndpointConfig(
            provider=provider,
            api_key=api_key_record.API_KEY,
        )

    def _get_openai_compatible_config(self, provider: str) -> LLMEndpointConfig:
        api_key_record = self.repo.get_api_key(provider)
        if not api_key_record or not api_key_record.API_KEY:
            raise HTTPException(detail="API Key Not Found", status_code=status.HTTP_404_NOT_FOUND)
        base_url = getattr(api_key_record, "BASE_URL", None)
        if not base_url:
            raise HTTPException(
                detail="openai-compatible base_url is not configured",
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        return LLMEndpointConfig(provider=provider, api_key=api_key_record.API_KEY, base_url=base_url)

    def _get_ollama_config(self, provider: str) -> LLMEndpointConfig:
        api_key_record = self.repo.get_api_key(provider)
        base_url = getattr(api_key_record, "BASE_URL", None) if api_key_record else None
        base_url = base_url or os.getenv("OLLAMA_BASE_URL")
        if not base_url:
            raise HTTPException(detail="ollama base_url is not configured", status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)
        return LLMEndpointConfig(provider=provider, base_url=base_url)


class CommonAPIKeyService:
    def __init__(self, db: Session = None):
        self.repo = LogAnalysisRepository(db=db)

    @staticmethod
    def map_key_to_res(key):
        return LLMAPIKey(
            seq=key.SEQ,
            provider=key.PROVIDER,
            api_key=key.API_KEY,
            base_url=getattr(key, "BASE_URL", None),
        )

    def get_api_key(self, provider: str | None = None):
        keys = self.repo.get_api_key(provider=provider)
        if not keys:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key found")

        if not provider:
            return [self.map_key_to_res(key) for key in keys]
        else:
            return [self.map_key_to_res(keys)]

    def post_api_key(self, provider: str, api_key: str | None, base_url: str | None = None):
        provider_value = CredentialService._provider_value(provider)
        if provider_value not in ("ollama", "openai-compatible"):
            base_url = None
        result = self.repo.post_api_key(provider=provider_value, api_key=api_key, base_url=base_url)
        return self.map_key_to_res(result)

    def delete_api_key(self, provider: str):
        result = self.repo.delete_api_key(provider=provider)
        if not result:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No API key found")
        return self.map_key_to_res(result)
