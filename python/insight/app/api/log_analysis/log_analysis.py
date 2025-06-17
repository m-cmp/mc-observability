from fastapi import APIRouter, Depends

from app.api.log_analysis.request.req import PostQueryBody, PostSessionBody, GetHistoryPath
from app.api.log_analysis.response.res import ResBodyLogAnalysisModel, LogAnalysisModel, ResBodyLogAnalysisSession, ResBodyLogAnalysisSessions, LogAnalysisSession, ResBodySessionHistory
from app.api.log_analysis.utils.utils import LogAnalysisService
from app.core.llm.ollama_client import OllamaClient
from app.core.dependencies.mcp import get_mcp_context
from app.core.mcp.mcp_context import MCPContext
from app.core.dependencies.db import get_db

from sqlalchemy.orm import Session

from config.ConfigManager import ConfigManager

router = APIRouter()


@router.get(
    path="/log-analysis/model",
    description="",
    # responses="",
    # response_model=ResBodyLogAnalysisModel,
    operation_id="GetLogAnalysisModel"
)
async def get_log_analysis_model():
    config = ConfigManager()
    model_info_config = config.get_model_config()

    log_analysis_service = LogAnalysisService()
    result = log_analysis_service.get_model_list(model_info_config)

    return ResBodyLogAnalysisModel(data=result)

@router.get(
    path="/log-analysis/session",
    description="",
    responses="",
    response_model=ResBodyLogAnalysisSessions,
    operation_id="GetLogAnalysisModelOptions"
)
async def get_log_analysis_session(db: Session = Depends(get_db)):
    log_analysis_service = LogAnalysisService(db=db)
    results = log_analysis_service.get_chat_session()
    return ResBodyLogAnalysisSessions(data=results)


@router.post(
    path="/log-analysis/session",
    description="",
    responses="",
    response_model=ResBodyLogAnalysisSession,
    operation_id="PostLogAnalysisSession"
)
async def post_log_analysis_session(
        body_params: PostSessionBody,
        db: Session = Depends(get_db)
):
    log_analysis_service = LogAnalysisService(db=db)
    result = log_analysis_service.create_chat_session(body=body_params)

    return ResBodyLogAnalysisSession(data=result)

@router.get(
    path="/log-analysis/session/{sessionId}/history",
    description="",
    responses="",
    response_model=ResBodySessionHistory,
    operation_id="GetLogAnalysisSessionHistory"
)
async def get_log_analysis_session_history(
        path_params: GetHistoryPath = Depends(),
        db: Session = Depends(get_db),
        mcp_context: MCPContext = Depends(get_mcp_context)
):
    log_analysis_service = LogAnalysisService(db=db, mcp_context=mcp_context)
    result = await log_analysis_service.get_chat_session_history(path=path_params)
    return ResBodySessionHistory(data=result)


@router.post(
    path="/log-analysis/query"
)
async def query_log_analysis(
        body_params: PostQueryBody,
        db: Session = Depends(get_db),
        mcp_context: MCPContext = Depends(get_mcp_context)
):
    session_id = 'dbe2cb8c-1d91-4361-80ed-9df8623cb857'
    response = await mcp_context.aquery(session_id, body_params.message)

    await mcp_context.aload_checkpoint(session_id)

    return response