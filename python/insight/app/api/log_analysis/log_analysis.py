from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from app.api.log_analysis.request.req import PostQueryBody, PostSessionBody, SessionIdPath, PostAPIKeyBody
from app.api.log_analysis.response.res import (
    ResBodyLogAnalysisModel,
    ResBodyLogAnalysisSession,
    ResBodyOpenAIAPIKey,
    ResBodyLogAnalysisSessions,
    ResBodySessionHistory,
    ResBodyQuery,
)
from app.api.log_analysis.utils.utils import LogAnalysisService, OpenAIAPIKeyService
from app.core.dependencies.mcp import get_mcp_context
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session
from config.ConfigManager import ConfigManager
import json
import asyncio


router = APIRouter()


@router.get(
    path="/log-analysis/model",
    description="",
    # responses="",
    response_model=ResBodyLogAnalysisModel,
    operation_id="GetLogAnalysisModelOptions",
)
async def get_log_analysis_model_options(db: Session = Depends(get_db)):
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
    # session_id = '921f5fc9-dbd8-4979-96a8-783b4c2fd3cd'
    log_analysis_service = LogAnalysisService(db=db, mcp_context=mcp_context)
    result = await log_analysis_service.query(body=body_params)

    return ResBodyQuery(data=result)


@router.post(
    path="/log-analysis/query/stream",
    description="스트리밍 방식으로 로그 분석 응답을 받습니다",
    operation_id="PostLogAnalysisQueryStream",
)
async def stream_log_analysis(body_params: PostQueryBody, db: Session = Depends(get_db), mcp_context=Depends(get_mcp_context)):
    """
    스트리밍 방식으로 로그 분석 응답을 실시간으로 받을 수 있는 엔드포인트입니다.
    Server-Sent Events (SSE) 형식으로 응답을 스트리밍합니다.
    """

    async def generate_stream():
        try:
            # 스트리밍 응답 시작
            yield f"data: {json.dumps({'type': 'start', 'message': '분석을 시작합니다...'}, ensure_ascii=False)}\n\n"
            await asyncio.sleep(0.1)  # 잠시 대기

            # 실제 분석 수행
            log_analysis_service = LogAnalysisService(db=db, mcp_context=mcp_context)
            result = await log_analysis_service.query(body=body_params)

            # 결과를 청크로 나누어 스트리밍
            message = result.message
            chunk_size = 30  # 30자씩 나누어 전송

            for i in range(0, len(message), chunk_size):
                chunk = message[i : i + chunk_size]
                yield f"data: {json.dumps({'type': 'chunk', 'message': chunk}, ensure_ascii=False)}\n\n"
                await asyncio.sleep(0.05)  # 스트리밍 효과를 위한 지연

            # 스트리밍 완료
            yield f"data: {json.dumps({'type': 'done', 'message': ''}, ensure_ascii=False)}\n\n"

        except Exception as e:
            # 오류 발생 시
            yield f"data: {json.dumps({'type': 'error', 'message': f'오류가 발생했습니다: {str(e)}'}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        generate_stream(),
        media_type="text/plain; charset=utf-8",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "*",
            "Access-Control-Allow-Methods": "*",
        },
    )


@router.get(
    path="/log-analysis/openai/api_keys",
    # description="",
    # responses="",
    summary="Get OpenAI API Key",
    response_model=ResBodyOpenAIAPIKey,
    operation_id="GetOpenAIAPIKey",
)
async def get_openai_api_key(db: Session = Depends(get_db)):
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
    service = OpenAIAPIKeyService(db=db)
    result = service.delete_key()
    return ResBodyOpenAIAPIKey(data=result)
