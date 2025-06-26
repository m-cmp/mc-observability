from fastapi import APIRouter, Depends

from app.api.log_analysis.request.req import PostQueryBody, PostSessionBody, SessionIdPath
from app.api.log_analysis.response.res import ResBodyLogAnalysisModel, ResBodyLogAnalysisSession, ResBodyLogAnalysisSessions, ResBodySessionHistory, ResBodyQuery
from app.api.log_analysis.utils.utils import LogAnalysisService
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
    # description="",
    # responses="",
    response_model=ResBodyLogAnalysisSessions,
    operation_id="GetLogAnalysisModelOptions"
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
    operation_id="PostLogAnalysisSession"
)
async def post_log_analysis_session(
        body_params: PostSessionBody,
        db: Session = Depends(get_db)
):
    log_analysis_service = LogAnalysisService(db=db)
    result = log_analysis_service.create_chat_session(body=body_params)

    return ResBodyLogAnalysisSession(data=result)

@router.delete(
    path="/log-analysis/session",
    # description="Delete session",
    # responses="",
    response_model=ResBodyLogAnalysisSession,
    operation_id="DeleteLogAnalysisSession"
)
async def delete_log_analysis_session(
        path_params: SessionIdPath = Depends(),
        db: Session = Depends(get_db)
):
    log_analysis_service = LogAnalysisService(db=db)
    result = log_analysis_service.delete_chat_session(path=path_params)

    return ResBodyLogAnalysisSession(data=result)



@router.get(
    path="/log-analysis/session/{sessionId}/history",
    # description="",
    # responses="",
    response_model=ResBodySessionHistory,
    operation_id="GetLogAnalysisSessionHistory"
)
async def get_log_analysis_session_history(
        path_params: SessionIdPath = Depends(),
        db: Session = Depends(get_db),
        mcp_context: MCPContext = Depends(get_mcp_context)
):
    log_analysis_service = LogAnalysisService(db=db, mcp_context=mcp_context)
    result = await log_analysis_service.get_chat_session_history(path=path_params)
    return ResBodySessionHistory(data=result)


@router.post(
    path="/log-analysis/query",
    # description="",
    # responses="",
    response_model=ResBodyQuery,
    operation_id="PostLogAnalysisQuery"

)
async def query_log_analysis(
        body_params: PostQueryBody,
        db: Session = Depends(get_db),
        mcp_context: MCPContext = Depends(get_mcp_context)
):
    # session_id = '921f5fc9-dbd8-4979-96a8-783b4c2fd3cd'
    log_analysis_service = LogAnalysisService(db=db, mcp_context=mcp_context)
    result = await log_analysis_service.query(body=body_params)

    return ResBodyQuery(data=result)