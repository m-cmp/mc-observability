from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.api.llm_analysis.request.req import PatchConnectionBody, PostConnectionBody, SetDefaultConnectionBody
from app.api.llm_analysis.response.res import (
    LLMConnectionModels,
    ResBodyLLMConnection,
    ResBodyLLMConnectionModels,
    ResBodyLLMConnections,
)
from app.api.llm_analysis.utils.llm_connection import LLMConnectionService
from app.core.dependencies.db import get_db

router = APIRouter()


@router.get("/llm/connections", response_model=ResBodyLLMConnections, operation_id="GetLLMConnections")
async def get_llm_connections(db: Session = Depends(get_db)):
    return ResBodyLLMConnections(data=LLMConnectionService(db).get_connections())


@router.get(
    "/llm/connections/{connection_id}",
    response_model=ResBodyLLMConnection,
    operation_id="GetLLMConnection",
)
async def get_llm_connection(connection_id: int, db: Session = Depends(get_db)):
    return ResBodyLLMConnection(data=LLMConnectionService(db).get_connection(connection_id))


@router.post(
    "/llm/connections",
    response_model=ResBodyLLMConnection,
    status_code=status.HTTP_201_CREATED,
    operation_id="PostLLMConnection",
)
async def post_llm_connection(body: PostConnectionBody, db: Session = Depends(get_db)):
    result = LLMConnectionService(db).create_connection(**body.model_dump())
    return ResBodyLLMConnection(data=result)


@router.patch(
    "/llm/connections/{connection_id}",
    response_model=ResBodyLLMConnection,
    operation_id="PatchLLMConnection",
)
async def patch_llm_connection(
    connection_id: int,
    body: PatchConnectionBody,
    db: Session = Depends(get_db),
):
    return ResBodyLLMConnection(data=LLMConnectionService(db).update_connection(connection_id, body))


@router.delete(
    "/llm/connections/{connection_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    operation_id="DeleteLLMConnection",
)
async def delete_llm_connection(connection_id: int, db: Session = Depends(get_db)):
    LLMConnectionService(db).delete_connection(connection_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get(
    "/llm/connections/{connection_id}/models",
    response_model=ResBodyLLMConnectionModels,
    operation_id="GetLLMConnectionModels",
)
async def get_llm_connection_models(connection_id: int, db: Session = Depends(get_db)):
    models = LLMConnectionService(db).get_models(connection_id)
    return ResBodyLLMConnectionModels(data=LLMConnectionModels(connection_id=connection_id, models=models))


@router.put(
    "/llm/connections/{connection_id}/default",
    response_model=ResBodyLLMConnection,
    operation_id="PutDefaultLLMConnection",
)
async def put_default_llm_connection(
    connection_id: int,
    body: SetDefaultConnectionBody,
    db: Session = Depends(get_db),
):
    result = LLMConnectionService(db).set_default_connection(connection_id, body.model_name)
    return ResBodyLLMConnection(data=result)
