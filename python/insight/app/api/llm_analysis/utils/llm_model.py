from app.api.llm_analysis.response.res import LLMModel
from app.api.llm_analysis.repo.repo import LogAnalysisRepository
from sqlalchemy.orm import Session
import os


class CommonModelService:
    PROVIDER_ENV_MAP = {
        "ollama": "OLLAMA_BASE_URL",
        "openai": "OPENAI_API_KEY",
        "google": "GOOGLE_API_KEY",
        "anthropic": "ANTHROPIC_API_KEY",
    }

    def __init__(self, db: Session = None):
        self.repo = LogAnalysisRepository(db=db)

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
            elif model_info["provider"] == "anthropic" and self.repo.get_anthropic_key():
                result.append(self.map_model_to_res(model_info))
            else:
                pass
        return result

    @staticmethod
    def map_model_to_res(model_info):
        return LLMModel(provider=model_info["provider"], model_name=model_info["model_name"])