from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.llm_analysis.description.llm_session import (
    delete_all_llm_sessions_description,
    delete_llm_session_description,
    get_llm_session_history_description,
    get_llm_sessions_description,
    post_llm_session_description,
)
from app.api.llm_analysis.request.req import PostSessionBody, SessionIdPath
from app.api.llm_analysis.response.res import (
    ResBodyLLMChatSession,
    ResBodyLLMChatSessions,
    ResBodySessionHistory,
)
from app.api.llm_analysis.utils.session import CommonSessionService
from app.core.dependencies.db import get_db
from app.core.dependencies.mcp import get_log_analysis_context

router = APIRouter()


@router.get(
    path="/llm/session",
    description=get_llm_sessions_description["api_description"],
    responses=get_llm_sessions_description["response"],
    response_model=ResBodyLLMChatSessions,
    operation_id="GetLLMChatSessions",
)
async def get_llm_chat_sessions(db: Session = Depends(get_db)):
    """
    Get all chat sessions.
    """
    session_service = CommonSessionService(db=db)
    results = session_service.get_sessions()
    return ResBodyLLMChatSessions(data=results)


@router.post(
    path="/llm/session",
    description=post_llm_session_description["api_description"],
    responses=post_llm_session_description["response"],
    response_model=ResBodyLLMChatSession,
    operation_id="PostLLMChatSession",
)
async def post_llm_chat_session(body_params: PostSessionBody, db: Session = Depends(get_db)):
    """
    Create a chat session.
    """
    session_service = CommonSessionService(db=db)
    result = session_service.create_chat_session(body=body_params)
    return ResBodyLLMChatSession(data=result)


@router.delete(
    path="/llm/session",
    description=delete_llm_session_description["api_description"],
    responses=delete_llm_session_description["response"],
    response_model=ResBodyLLMChatSession,
    operation_id="DeleteLLMChatSession",
)
async def delete_llm_chat_session(path_params: SessionIdPath = Depends(), db: Session = Depends(get_db)):
    """
    Delete a chat session.
    """
    session_service = CommonSessionService(db=db)
    result = session_service.delete_chat_session(path=path_params)
    return ResBodyLLMChatSession(data=result)


@router.delete(
    path="/llm/sessions",
    description=delete_all_llm_sessions_description["api_description"],
    responses=delete_all_llm_sessions_description["response"],
    response_model=ResBodyLLMChatSessions,
    operation_id="DeleteAllLLMChatSessions",
)
async def delete_all_llm_chat_sessions(db: Session = Depends(get_db)):
    """
    Delete all chat sessions.
    """
    session_service = CommonSessionService(db=db)
    result = session_service.delete_all_chat_sessions()
    return ResBodyLLMChatSessions(data=result)


@router.get(
    path="/llm/session/{sessionId}/history",
    description=get_llm_session_history_description["api_description"],
    responses=get_llm_session_history_description["response"],
    response_model=ResBodySessionHistory,
    operation_id="GetLLMSessionHistory",
)
async def get_llm_session_history(
    path_params: SessionIdPath = Depends(), db: Session = Depends(get_db), mcp_context=Depends(get_log_analysis_context)
):
    """
    Get chat session history.
    """
    session_service = CommonSessionService(db=db, mcp_context=mcp_context)
    result = await session_service.get_chat_session_history(path=path_params)
    return ResBodySessionHistory(data=result)
