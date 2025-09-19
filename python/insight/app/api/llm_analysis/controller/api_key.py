from fastapi import APIRouter, Depends
from app.api.llm_analysis.request.req import PostAPIKeyBody
from app.api.llm_analysis.response.res import (
    ResBodyOpenAIAPIKey,
    ResBodyGoogleAPIKey,
    ResBodyAnthropicAPIKey,
)
from app.api.llm_analysis.utils.llm_api_key import CommonAPIKeyService
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session

router = APIRouter()


# OpenAI API Key endpoints
@router.get(
    path="/llm/openai/api_keys",
    summary="Get OpenAI API Key",
    response_model=ResBodyOpenAIAPIKey,
    operation_id="GetLLMAnalysisOpenAIAPIKey",
)
async def get_openai_api_key(db: Session = Depends(get_db)):
    """
    Get OpenAI API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.get_openai_key()
    return ResBodyOpenAIAPIKey(data=result)


@router.post(
    path="/llm/openai/api_keys",
    summary="Post OpenAI API Key",
    response_model=ResBodyOpenAIAPIKey,
    operation_id="PostLLMAnalysisOpenAIAPIKey",
)
async def post_openai_api_key(body_params: PostAPIKeyBody, db: Session = Depends(get_db)):
    """
    Save OpenAI API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.post_openai_key(body_params.api_key)
    return ResBodyOpenAIAPIKey(data=result)


@router.delete(
    path="/llm/openai/api_keys",
    summary="Delete OpenAI API Key",
    response_model=ResBodyOpenAIAPIKey,
    operation_id="DeleteLLMAnalysisOpenAIAPIKey",
)
async def delete_openai_api_key(db: Session = Depends(get_db)):
    """
    Delete OpenAI API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.delete_openai_key()
    return ResBodyOpenAIAPIKey(data=result)


# Google API Key endpoints
@router.get(
    path="/llm/google/api_keys",
    summary="Get Google API Key",
    response_model=ResBodyGoogleAPIKey,
    operation_id="GetLLMAnalysisGoogleAPIKey",
)
async def get_google_api_key(db: Session = Depends(get_db)):
    """
    Get Google API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.get_google_key()
    return ResBodyGoogleAPIKey(data=result)


@router.post(
    path="/llm/google/api_keys",
    summary="Post Google API Key",
    response_model=ResBodyGoogleAPIKey,
    operation_id="PostLLMAnalysisGoogleAPIKey",
)
async def post_google_api_key(body_params: PostAPIKeyBody, db: Session = Depends(get_db)):
    """
    Save Google API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.post_google_key(body_params.api_key)
    return ResBodyGoogleAPIKey(data=result)


@router.delete(
    path="/llm/google/api_keys",
    summary="Delete Google API Key",
    response_model=ResBodyGoogleAPIKey,
    operation_id="DeleteLLMAnalysisGoogleAPIKey",
)
async def delete_google_api_key(db: Session = Depends(get_db)):
    """
    Delete Google API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.delete_google_key()
    return ResBodyGoogleAPIKey(data=result)


# Anthropic API Key endpoints
@router.get(
    path="/llm/anthropic/api_keys",
    summary="Get Anthropic API Key",
    response_model=ResBodyAnthropicAPIKey,
    operation_id="GetLLMAnalysisAnthropicAPIKey",
)
async def get_anthropic_api_key(db: Session = Depends(get_db)):
    """
    Get Anthropic API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.get_anthropic_key()
    return ResBodyAnthropicAPIKey(data=result)


@router.post(
    path="/llm/anthropic/api_keys",
    summary="Post Anthropic API Key",
    response_model=ResBodyAnthropicAPIKey,
    operation_id="PostLLMAnalysisAnthropicAPIKey",
)
async def post_anthropic_api_key(body_params: PostAPIKeyBody, db: Session = Depends(get_db)):
    """
    Save Anthropic API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.post_anthropic_key(body_params.api_key)
    return ResBodyAnthropicAPIKey(data=result)


@router.delete(
    path="/llm/anthropic/api_keys",
    summary="Delete Anthropic API Key",
    response_model=ResBodyAnthropicAPIKey,
    operation_id="DeleteLLMAnalysisAnthropicAPIKey",
)
async def delete_anthropic_api_key(db: Session = Depends(get_db)):
    """
    Delete Anthropic API key.
    """
    service = CommonAPIKeyService(db=db)
    result = service.delete_anthropic_key()
    return ResBodyAnthropicAPIKey(data=result)
