from fastapi import HTTPException, status
from langchain_core.language_models import BaseChatModel
from langchain_ollama import ChatOllama
from langchain_openai import ChatOpenAI

from app.api.llm_analysis.request.req import is_official_openai_base_url
from app.api.llm_analysis.utils.llm_connection import LLMConnectionService


def create_chat_model(repo, model_name: str, connection_id: int | None) -> BaseChatModel:
    if connection_id is None:
        raise HTTPException(
            detail="Session is not associated with an LLM connection",
            status_code=status.HTTP_409_CONFLICT,
        )
    connection = repo.get_connection_by_id(connection_id)
    if not connection:
        raise HTTPException(
            detail="LLM connection not found",
            status_code=status.HTTP_404_NOT_FOUND,
        )
    if not connection.ENABLED:
        raise HTTPException(
            detail="LLM connection is disabled",
            status_code=status.HTTP_409_CONFLICT,
        )

    provider = connection.PROVIDER
    base_url = connection.BASE_URL
    if provider not in ("ollama", "openai"):
        raise HTTPException(
            detail=f"Unsupported provider: {provider}",
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    if provider == "ollama":
        if not base_url:
            raise HTTPException(
                detail="ollama base_url is not configured",
                status_code=status.HTTP_409_CONFLICT,
            )
        # num_ctx is per-request and never reported back, so a declared window has to be
        # sent to be real. Unset: Ollama sizes from VRAM and callers fall back.
        return ChatOllama(
            model=model_name,
            base_url=base_url,
            temperature=0,
            **({"num_ctx": connection.CONTEXT_LENGTH} if connection.CONTEXT_LENGTH else {}),
        )

    api_key = LLMConnectionService(repo.db).decrypt_api_key(connection)
    if not api_key and is_official_openai_base_url(base_url):
        raise HTTPException(
            detail="API key is required for the default OpenAI endpoint",
            status_code=status.HTTP_409_CONFLICT,
        )
    model_options = {}
    if not model_name.lower().startswith(("gpt-3", "gpt-4")):
        model_options = {
            "reasoning": {"effort": "high"},
            "use_responses_api": True,
        }
    return ChatOpenAI(
        model=model_name,
        api_key=api_key or "not-required",
        base_url=base_url,
        **model_options,
    )
