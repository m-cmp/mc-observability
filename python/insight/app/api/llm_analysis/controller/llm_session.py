from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.api.llm_analysis.request.req import PostSessionBody, SessionIdPath
from app.api.llm_analysis.response.res import (
    ResBodyLLMChatSession,
    ResBodyLLMChatSessions,
    ResBodySessionHistory,
)
from app.api.llm_analysis.utils.session import CommonSessionService
from app.core.dependencies.db import get_db

router = APIRouter()


@router.get(
    path="/llm/sessions",
    response_model=ResBodyLLMChatSessions,
    operation_id="GetLLMSessions",
)
async def get_llm_sessions(db: Session = Depends(get_db)):
    return ResBodyLLMChatSessions(data=CommonSessionService(db=db).get_sessions())


@router.post(
    path="/llm/sessions",
    response_model=ResBodyLLMChatSession,
    status_code=status.HTTP_201_CREATED,
    operation_id="PostLLMSession",
)
async def post_llm_session(body_params: PostSessionBody, db: Session = Depends(get_db)):
    return ResBodyLLMChatSession(data=CommonSessionService(db=db).create_chat_session(body=body_params))


@router.delete(
    path="/llm/sessions/{session_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    operation_id="DeleteLLMSession",
)
async def delete_llm_session(session_id: str, db: Session = Depends(get_db)):
    CommonSessionService(db=db).delete_chat_session(path=SessionIdPath(sessionId=session_id))
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.delete(
    path="/llm/sessions",
    response_model=ResBodyLLMChatSessions,
    operation_id="DeleteAllLLMChatSessions",
)
async def delete_all_llm_chat_sessions(db: Session = Depends(get_db)):
    session_service = CommonSessionService(db=db)
    result = session_service.delete_all_chat_sessions()
    return ResBodyLLMChatSessions(data=result)


@router.get(
    path="/llm/sessions/{session_id}/history",
    response_model=ResBodySessionHistory,
    operation_id="GetLLMSessionHistory",
)
async def get_llm_session_history(
    session_id: str,
    db: Session = Depends(get_db),
):
    result = await CommonSessionService(db=db).get_chat_session_history(
        path=SessionIdPath(sessionId=session_id)
    )
    return ResBodySessionHistory(data=result)
