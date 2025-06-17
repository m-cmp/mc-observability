from fastapi import APIRouter, Depends

from app.api.log_analysis.request.req import LogAnalysisQueryBody, LogAnalysisSessionBody
from app.api.log_analysis.response.res import ResBodyLogAnalysisModel, LogAnalysisModel, ResBodyLogAnalysisSession, ResBodyLogAnalysisSessions, LogAnalysisSession
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
        body_params: LogAnalysisSessionBody,
        db: Session = Depends(get_db)
):
    log_analysis_service = LogAnalysisService(db=db)
    result = log_analysis_service.create_chat_session(body_params)

    return ResBodyLogAnalysisSession(data=result)


@router.post(
    path="/log-analysis/query"
)
async def query_log_analysis(
        body: LogAnalysisQueryBody,
        db: Session = Depends(get_db),
        mcp_context: MCPContext = Depends(get_mcp_context)
):
    print(body.user_id)
    print(body.message)

    response = await mcp_context.query(body.message, body.user_id)

    print(response)
    return response