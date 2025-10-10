from typing import Optional

from fastapi import APIRouter, Depends
from app.api.llm_analysis.request.req import GetAPIKeyPath, PostAPIKeyBody, GetAPIKeyFilter, DeleteAPIKeyFilter
from app.api.llm_analysis.response.res import (
    ResBodyLLMAPIKey,
    ResBodyLLMAPIKeys
)
from app.api.llm_analysis.description.api_key import get_api_key_description, post_api_key_description, delete_api_key_description
from app.api.llm_analysis.utils.llm_api_key import CommonAPIKeyService
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session

router = APIRouter()

@router.get(
    path="/llm/api-keys",
    description=get_api_key_description['api_description'],
    responses=get_api_key_description['response'],
    response_model=ResBodyLLMAPIKeys,
    operation_id="GetLLMAPIKeys"
)
async def get_llm_api_key(
        db: Session = Depends(get_db),
        query_params: GetAPIKeyFilter = Depends()
):
    service = CommonAPIKeyService(db=db)
    result = service.get_api_key(provider=query_params.provider)
    return ResBodyLLMAPIKeys(data=result)

@router.post(
    path="/llm/api-keys",
    description=post_api_key_description['api_description'],
    responses=post_api_key_description['response'],
    response_model=ResBodyLLMAPIKey,
    operation_id="PostLLMAPIKeys"
)
async def post_llm_api_key(
        body_params: PostAPIKeyBody,
        db: Session = Depends(get_db)
):
    service = CommonAPIKeyService(db=db)
    result = service.post_api_key(provider=body_params.provider, api_key=body_params.api_key)
    return ResBodyLLMAPIKey(data=result)


@router.delete(
    path="/llm/api-keys",
    description=delete_api_key_description['api_description'],
    responses=delete_api_key_description['response'],
    response_model=ResBodyLLMAPIKey,
    operation_id="DeleteLLMAPIKeys"
)
async def delete_llm_api_key(
        db: Session = Depends(get_db),
        query_params: DeleteAPIKeyFilter = Depends()
):
    service = CommonAPIKeyService(db=db)
    result = service.delete_api_key(provider=query_params.provider)
    return ResBodyLLMAPIKey(data=result)
