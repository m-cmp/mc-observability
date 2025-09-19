from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from app.api.llm_analysis.request.req import PostQueryBody
from app.api.llm_analysis.response.res import ResBodyQuery
from app.api.llm_analysis.utils.log_analysis import LogQueryService
from app.core.dependencies.mcp import get_mcp_context
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session

router = APIRouter()


@router.post(
    path="/log-analysis/query",
    response_model=ResBodyQuery,
    operation_id="PostLogAnalysisQuery",
)
async def query_log_analysis(body_params: PostQueryBody, db: Session = Depends(get_db),
                             mcp_context=Depends(get_mcp_context)):
    """
    Submit a query to the log analysis chat session.
    """
    log_query_service = LogQueryService(db=db, mcp_context=mcp_context)
    result = await log_query_service.query(body=body_params)
    return ResBodyQuery(data=result)

# @router.post(
#     path="/log-analysis/query/stream",
#     summary="Stream Log Analysis Query",
#     operation_id="PostLogAnalysisQueryStream",
# )
# async def query_log_analysis_stream(body_params: PostQueryBody, db: Session = Depends(get_db), mcp_context=Depends(get_mcp_context)):
#     """
#     Submit a query to the log analysis chat session. (Streaming)
#     """
#     service = LogQueryService(db=db, mcp_context=mcp_context)
#     generator = await service.query_stream(body=body_params)
#     return StreamingResponse(generator, media_type="text/event-stream", headers={"Cache-Control": "no-cache", "Connection": "keep-alive"})
