from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from app.api.llm_analysis.request.req import PostQueryBody
from app.api.llm_analysis.response.res import ResBodyQuery
from app.api.llm_analysis.utils.alert_analysis import AlarmQueryService
from app.core.dependencies.mcp import get_mcp_context
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session

router = APIRouter()


@router.post(
    path="/alarm-analysis/query",
    response_model=ResBodyQuery,
    operation_id="PostAlarmAnalysisQuery",
)
async def query_alarm_analysis(body_params: PostQueryBody, db: Session = Depends(get_db),
                               mcp_context=Depends(get_mcp_context)):
    """
    Submit a query to the alarm analysis chat session.
    """
    alarm_query_service = AlarmQueryService(db=db, mcp_context=mcp_context)
    result = await alarm_query_service.query(body=body_params)
    return ResBodyQuery(data=result)

# @router.post(
#     path="/alarm-analysis/query/stream",
#     summary="Stream Alarm Analysis Query",
#     operation_id="PostAlarmAnalysisQueryStream",
# )
# async def query_alarm_analysis_stream(body_params: PostQueryBody, db: Session = Depends(get_db), mcp_context=Depends(get_mcp_context)):
#     """
#     Submit a query to the alarm analysis chat session. (Streaming)
#     """
#     service = AlarmQueryService(db=db, mcp_context=mcp_context)
#     generator = await service.query_stream(body=body_params)
#     return StreamingResponse(generator, media_type="text/event-stream", headers={"Cache-Control": "no-cache", "Connection": "keep-alive"})
