from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from app.api.log_analysis.request.req import PostQueryBody, PostSessionBody, SessionIdPath, PostAPIKeyBody
from app.api.log_analysis.response.res import (
    ResBodyLogAnalysisModel,
    ResBodyLogAnalysisSession,
    ResBodyOpenAIAPIKey,
    ResBodyGoogleAPIKey,
    ResBodyLogAnalysisSessions,
    ResBodySessionHistory,
    ResBodyQuery,
)
from app.api.log_analysis.utils.utils import LogAnalysisService, OpenAIAPIKeyService, GoogleAPIKeyService
from app.core.dependencies.mcp import get_mcp_context
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session
from config.ConfigManager import ConfigManager


router = APIRouter()


@router.get(
    path="/log-analysis/model",
    description="",
    # responses="",
    response_model=ResBodyLogAnalysisModel,
    operation_id="GetLogAnalysisModelOptions",
)
async def get_log_analysis_model_options(db: Session = Depends(get_db)):
    """
    모델 옵션을 조회합니다.
    """
    config = ConfigManager()
    model_info_config = config.get_model_config()

    log_analysis_service = LogAnalysisService(db=db)
    result = log_analysis_service.get_model_list(model_info_config)

    return ResBodyLogAnalysisModel(data=result)


@router.get(
    path="/log-analysis/session",
    # description="",
    # responses="",
    response_model=ResBodyLogAnalysisSessions,
    operation_id="GetLogAnalysisSessions",
)
async def get_log_analysis_session(db: Session = Depends(get_db)):
    """
    모든 채팅 세션을 조회합니다.
    """
    log_analysis_service = LogAnalysisService(db=db)
    results = log_analysis_service.get_sessions()
    return ResBodyLogAnalysisSessions(data=results)


@router.post(
    path="/log-analysis/session",
    # description="",
    # responses="",
    response_model=ResBodyLogAnalysisSession,
    operation_id="PostLogAnalysisSession",
)
async def post_log_analysis_session(body_params: PostSessionBody, db: Session = Depends(get_db)):
    """
    채팅 세션을 생성합니다.
    """
    log_analysis_service = LogAnalysisService(db=db)
    result = log_analysis_service.create_chat_session(body=body_params)

    return ResBodyLogAnalysisSession(data=result)


@router.delete(
    path="/log-analysis/session",
    # description="Delete session",
    # responses="",
    response_model=ResBodyLogAnalysisSession,
    operation_id="DeleteLogAnalysisSession",
)
async def delete_log_analysis_session(path_params: SessionIdPath = Depends(), db: Session = Depends(get_db)):
    """
    채팅 세션을 삭제합니다.
    """
    log_analysis_service = LogAnalysisService(db=db)
    result = log_analysis_service.delete_chat_session(path=path_params)

    return ResBodyLogAnalysisSession(data=result)


@router.delete(
    path="/log-analysis/sessions",
    # description="Delete all sessions",
    # responses="",
    response_model=ResBodyLogAnalysisSessions,
    operation_id="DeleteAllLogAnalysisSessions",
)
async def delete_all_log_analysis_session(db: Session = Depends(get_db)):
    """
    모든 채팅 세션을 삭제합니다.
    """
    log_analysis_service = LogAnalysisService(db=db)
    result = log_analysis_service.delete_all_chat_sessions()

    return ResBodyLogAnalysisSessions(data=result)


@router.get(
    path="/log-analysis/session/{sessionId}/history",
    # description="",
    # responses="",
    response_model=ResBodySessionHistory,
    operation_id="GetLogAnalysisSessionHistory",
)
async def get_log_analysis_session_history(path_params: SessionIdPath = Depends(), db: Session = Depends(get_db), mcp_context=Depends(get_mcp_context)):
    """
    채팅 세션의 대화 내역을 조회합니다.
    """
    log_analysis_service = LogAnalysisService(db=db, mcp_context=mcp_context)
    result = await log_analysis_service.get_chat_session_history(path=path_params)
    return ResBodySessionHistory(data=result)


@router.post(
    path="/log-analysis/query",
    # description="",
    # responses="",
    response_model=ResBodyQuery,
    operation_id="PostLogAnalysisQuery",
)
async def query_log_analysis(body_params: PostQueryBody, db: Session = Depends(get_db), mcp_context=Depends(get_mcp_context)):
    """
    채팅 세션에 질의를 진행합니다.
    """
    # session_id = '921f5fc9-dbd8-4979-96a8-783b4c2fd3cd'
    log_analysis_service = LogAnalysisService(db=db, mcp_context=mcp_context)
    result = await log_analysis_service.query(body=body_params)

    return ResBodyQuery(data=result)


@router.post(
    path="/log-analysis/query/stream",
    # description="Streaming query via SSE",
    # responses="",
    summary="Stream Log Analysis Query",
    operation_id="PostLogAnalysisQueryStream",
)
async def query_log_analysis_stream(body_params: PostQueryBody, db: Session = Depends(get_db), mcp_context=Depends(get_mcp_context)):
    """
    채팅 세션에 질의를 진행합니다. (스트리밍)
    """
    service = LogAnalysisService(db=db, mcp_context=mcp_context)
    generator = await service.query_stream(body=body_params)
    return StreamingResponse(generator, media_type="text/event-stream", headers={"Cache-Control": "no-cache", "Connection": "keep-alive"})


@router.get(
    path="/log-analysis/openai/api_keys",
    # description="",
    # responses="",
    summary="Get OpenAI API Key",
    response_model=ResBodyOpenAIAPIKey,
    operation_id="GetOpenAIAPIKey",
)
async def get_openai_api_key(db: Session = Depends(get_db)):
    """
    OpenAI API 키를 조회합니다.
    """
    service = OpenAIAPIKeyService(db=db)
    result = service.get_key()
    return ResBodyOpenAIAPIKey(data=result)


@router.post(
    path="/log-analysis/openai/api_keys",
    # description="",
    # responses="",
    summary="Post OpenAI API Key",
    response_model=ResBodyOpenAIAPIKey,
    operation_id="PostOpenAIAPIKey",
)
async def post_openai_api_key(body_params: PostAPIKeyBody, db: Session = Depends(get_db)):
    """
    OpenAI API 키를 저장합니다.
    """
    service = OpenAIAPIKeyService(db=db)
    result = service.post_key(body_params.api_key)
    return ResBodyOpenAIAPIKey(data=result)


@router.delete(
    path="/log-analysis/openai/api_keys",
    # description="",
    # responses="",
    summary="Delete OpenAI API Key",
    response_model=ResBodyOpenAIAPIKey,
    operation_id="DeleteOpenAIAPIKey",
)
async def delete_openai_api_key(db: Session = Depends(get_db)):
    """
    OpenAI API 키를 삭제합니다.
    """
    service = OpenAIAPIKeyService(db=db)
    result = service.delete_key()
    return ResBodyOpenAIAPIKey(data=result)


@router.get(
    path="/log-analysis/google/api_keys",
    # description="",
    # responses="",
    summary="Get Google API Key",
    response_model=ResBodyGoogleAPIKey,
    operation_id="GetGoogleAPIKey",
)
async def get_google_api_key(db: Session = Depends(get_db)):
    """
    Google API 키를 조회합니다.
    """
    service = GoogleAPIKeyService(db=db)
    result = service.get_key()
    return ResBodyGoogleAPIKey(data=result)


@router.post(
    path="/log-analysis/google/api_keys",
    # description="",
    # responses="",
    summary="Post Google API Key",
    response_model=ResBodyGoogleAPIKey,
    operation_id="PostGoogleAPIKey",
)
async def post_google_api_key(body_params: PostAPIKeyBody, db: Session = Depends(get_db)):
    """
    Google API 키를 저장합니다.
    """
    service = GoogleAPIKeyService(db=db)
    result = service.post_key(body_params.api_key)
    return ResBodyGoogleAPIKey(data=result)


@router.delete(
    path="/log-analysis/google/api_keys",
    # description="",
    # responses="",
    summary="Delete Google API Key",
    response_model=ResBodyGoogleAPIKey,
    operation_id="DeleteGoogleAPIKey",
)
async def delete_google_api_key(db: Session = Depends(get_db)):
    """
    Google API 키를 삭제합니다.
    """
    service = GoogleAPIKeyService(db=db)
    result = service.delete_key()
    return ResBodyGoogleAPIKey(data=result)
