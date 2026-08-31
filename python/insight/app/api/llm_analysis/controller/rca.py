from fastapi import APIRouter, Depends, Request, Response, status
from sqlalchemy.orm import Session

from app.api.llm_analysis.description.rca import (
    get_rca_record_detail_description,
    get_rca_records_description,
    post_rca_query_description,
)
from app.api.llm_analysis.request.req import (
    PatchRcaScheduleBody,
    PostRcaQueryBody,
    PostRcaScheduleBody,
    RcaAnalysisIdPath,
    RcaRecordFilter,
    RcaScheduleIdPath,
)
from app.api.llm_analysis.response.res import (
    ResBodyRcaQuery,
    ResBodyRcaRecord,
    ResBodyRcaRecords,
    ResBodyRcaSchedule,
    ResBodyRcaSchedules,
)
from app.api.llm_analysis.utils.rca import RcaAnalysisService
from app.api.llm_analysis.utils.rca_schedule import RcaScheduleService
from app.core.dependencies.db import get_db

router = APIRouter()


@router.post(
    path="/rca/query",
    description=post_rca_query_description["api_description"],
    responses=post_rca_query_description["response"],
    response_model=ResBodyRcaQuery,
    operation_id="PostRcaQuery",
    status_code=status.HTTP_202_ACCEPTED,
)
async def query_rca(
    request: Request,
    body_params: PostRcaQueryBody,
    db: Session = Depends(get_db),
):
    # Accepted, not completed: the record comes back at once and the analysis runs in the
    # background; clients read the result from GET /rca/records/{id}.
    service = RcaAnalysisService(db=db, rca_graph=request.app.state.rca_graph)
    return ResBodyRcaQuery(data=await service.submit_analysis(body_params))


@router.get(
    path="/rca/records",
    description=get_rca_records_description["api_description"],
    responses=get_rca_records_description["response"],
    response_model=ResBodyRcaRecords,
    operation_id="GetRcaRecords",
)
async def get_rca_records(
    query_params: RcaRecordFilter = Depends(),
    db: Session = Depends(get_db),
):
    service = RcaAnalysisService(db=db)
    return ResBodyRcaRecords(data=service.list_records(query_params))


@router.get(
    path="/rca/records/{analysis_id}",
    description=get_rca_record_detail_description["api_description"],
    responses=get_rca_record_detail_description["response"],
    response_model=ResBodyRcaRecord,
    operation_id="GetRcaRecord",
)
async def get_rca_record(
    path_params: RcaAnalysisIdPath = Depends(),
    db: Session = Depends(get_db),
):
    service = RcaAnalysisService(db=db)
    return ResBodyRcaRecord(data=service.get_record(path_params.analysis_id))


@router.get(
    path="/rca/schedules",
    response_model=ResBodyRcaSchedules,
    operation_id="GetRcaSchedules",
)
async def get_rca_schedules(db: Session = Depends(get_db)):
    return ResBodyRcaSchedules(data=RcaScheduleService(db).list_schedules())


@router.post(
    path="/rca/schedules",
    status_code=status.HTTP_201_CREATED,
    response_model=ResBodyRcaSchedule,
    operation_id="PostRcaSchedule",
)
async def post_rca_schedule(body_params: PostRcaScheduleBody, db: Session = Depends(get_db)):
    return ResBodyRcaSchedule(data=RcaScheduleService(db).create(body_params))


@router.patch(
    path="/rca/schedules/{schedule_id}",
    response_model=ResBodyRcaSchedule,
    operation_id="PatchRcaSchedule",
)
async def patch_rca_schedule(
    body_params: PatchRcaScheduleBody,
    path_params: RcaScheduleIdPath = Depends(),
    db: Session = Depends(get_db),
):
    return ResBodyRcaSchedule(data=RcaScheduleService(db).update(path_params.schedule_id, body_params))


@router.delete(
    path="/rca/schedules/{schedule_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    operation_id="DeleteRcaSchedule",
)
async def delete_rca_schedule(
    path_params: RcaScheduleIdPath = Depends(),
    db: Session = Depends(get_db),
):
    RcaScheduleService(db).delete(path_params.schedule_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
