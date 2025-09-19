from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from app.api.llm_analysis.request.req import PostQueryBody
from app.api.llm_analysis.response.res import ResBodyQuery
from app.api.llm_analysis.utils.alert_analysis import AlertQueryService
from app.core.dependencies.mcp import get_mcp_context
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session

router = APIRouter()


@router.post(
    path="/alert-analysis/query",
    response_model=ResBodyQuery,
    operation_id="PostAlertAnalysisQuery",
)
async def query_alert_analysis(body_params: PostQueryBody, db: Session = Depends(get_db),
                               mcp_context=Depends(get_mcp_context)):
    """
    Submit a query to the alert analysis chat session.
    """
    alert_query_service = AlertQueryService(db=db, mcp_context=mcp_context)
    result = await alert_query_service.query(body=body_params)
    return ResBodyQuery(data=result)

# @router.post(
#     path="/alert-analysis/query/stream",
#     summary="Stream Alert Analysis Query",
#     operation_id="PostAlertAnalysisQueryStream",
# )
# async def query_alert_analysis_stream(body_params: PostQueryBody, db: Session = Depends(get_db), mcp_context=Depends(get_mcp_context)):
#     """
#     Submit a query to the alert analysis chat session. (Streaming)
#     """
#     service = AlertQueryService(db=db, mcp_context=mcp_context)
#     generator = await service.query_stream(body=body_params)
#     return StreamingResponse(generator, media_type="text/event-stream", headers={"Cache-Control": "no-cache", "Connection": "keep-alive"})
