import os
from typing import ClassVar

from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from app.api.llm_analysis.response.res import LLMModel


class CommonModelService:
    PROVIDER_ENV_MAP: ClassVar[dict[str, str]] = {
        "ollama": "OLLAMA_BASE_URL",
        "openai": "OPENAI_API_KEY",
        "openai-compatible": "OPENAI_COMPATIBLE_API_KEY",
        "google": "GOOGLE_API_KEY",
        "anthropic": "ANTHROPIC_API_KEY",
    }

    def __init__(self, db: Session = None):
        self.repo = LogAnalysisRepository(db=db)

    def get_model_list(self, model_info_config):
        result = []
        for model_info in model_info_config:
            provider = model_info["provider"]
            api_key = self.repo.get_api_key(provider=provider)
            if provider == "ollama" and (
                os.getenv(self.PROVIDER_ENV_MAP[provider]) or (api_key and getattr(api_key, "BASE_URL", None))
            ):
                result.append(self.map_model_to_res(model_info))
            elif provider == "openai-compatible" and api_key and api_key.API_KEY and getattr(api_key, "BASE_URL", None):
                result.append(self.map_model_to_res(model_info))
            elif provider in ("openai", "google", "anthropic") and api_key:
                result.append(self.map_model_to_res(model_info))
            else:
                pass
        return result

    @staticmethod
    def map_model_to_res(model_info):
        return LLMModel(provider=model_info["provider"], model_name=model_info["model_name"])
