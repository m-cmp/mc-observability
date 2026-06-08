from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.api.llm_analysis.description.server_error_analysis import (
    get_server_error_record_detail_description,
    get_server_error_records_description,
    post_server_error_detect_description,
    post_server_error_query_description,
    post_server_error_rerun_description,
)
from app.api.llm_analysis.request.req import (
    PostServerErrorDetectBody,
    PostServerErrorQueryBody,
    ServerErrorAnalysisIdPath,
    ServerErrorRecordFilter,
)
from app.api.llm_analysis.response.res import (
    ResBodyServerErrorDetect,
    ResBodyServerErrorQuery,
    ResBodyServerErrorRecord,
    ResBodyServerErrorRecords,
)
from app.api.llm_analysis.utils.server_error_analysis import ServerErrorAnalysisService
from app.core.dependencies.db import get_db
from app.core.dependencies.mcp import get_server_error_analysis_context

router = APIRouter()


@router.post(
    path="/server-error-analysis/detect",
    description=post_server_error_detect_description["api_description"],
    responses=post_server_error_detect_description["response"],
    response_model=ResBodyServerErrorDetect,
    operation_id="PostServerErrorAnalysisDetect",
)
async def detect_server_error_analysis(
    request: Request,
    body_params: PostServerErrorDetectBody,
    db: Session = Depends(get_db),
    mcp_context=Depends(get_server_error_analysis_context),
):
    service = ServerErrorAnalysisService(
        db=db,
        mcp_manager=mcp_context,
        server_error_graph=request.app.state.server_error_graph_runtime.graph,
    )
    return ResBodyServerErrorDetect(data=await service.detect(body_params))


@router.post(
    path="/server-error-analysis/query",
    description=post_server_error_query_description["api_description"],
    responses=post_server_error_query_description["response"],
    response_model=ResBodyServerErrorQuery,
    operation_id="PostServerErrorAnalysisQuery",
)
async def query_server_error_analysis(
    request: Request,
    body_params: PostServerErrorQueryBody,
    db: Session = Depends(get_db),
    mcp_context=Depends(get_server_error_analysis_context),
):
    service = ServerErrorAnalysisService(
        db=db,
        mcp_manager=mcp_context,
        server_error_graph=request.app.state.server_error_graph_runtime.graph,
    )
    return ResBodyServerErrorQuery(data=await service.query(body_params))


@router.get(
    path="/server-error-analysis/records",
    description=get_server_error_records_description["api_description"],
    responses=get_server_error_records_description["response"],
    response_model=ResBodyServerErrorRecords,
    operation_id="GetServerErrorAnalysisRecords",
)
async def get_server_error_analysis_records(
    query_params: ServerErrorRecordFilter = Depends(),
    db: Session = Depends(get_db),
):
    service = ServerErrorAnalysisService(db=db)
    return ResBodyServerErrorRecords(data=service.list_records(query_params))


@router.get(
    path="/server-error-analysis/records/{analysis_id}",
    description=get_server_error_record_detail_description["api_description"],
    responses=get_server_error_record_detail_description["response"],
    response_model=ResBodyServerErrorRecord,
    operation_id="GetServerErrorAnalysisRecord",
)
async def get_server_error_analysis_record(
    path_params: ServerErrorAnalysisIdPath = Depends(),
    db: Session = Depends(get_db),
):
    service = ServerErrorAnalysisService(db=db)
    return ResBodyServerErrorRecord(data=service.get_record(path_params.analysis_id))


@router.post(
    path="/server-error-analysis/records/{analysis_id}/rerun",
    description=post_server_error_rerun_description["api_description"],
    responses=post_server_error_rerun_description["response"],
    response_model=ResBodyServerErrorQuery,
    operation_id="PostServerErrorAnalysisRerun",
)
async def rerun_server_error_analysis(
    request: Request,
    path_params: ServerErrorAnalysisIdPath = Depends(),
    db: Session = Depends(get_db),
    mcp_context=Depends(get_server_error_analysis_context),
):
    service = ServerErrorAnalysisService(
        db=db,
        mcp_manager=mcp_context,
        server_error_graph=request.app.state.server_error_graph_runtime.graph,
    )
    return ResBodyServerErrorQuery(data=await service.rerun(path_params.analysis_id))
