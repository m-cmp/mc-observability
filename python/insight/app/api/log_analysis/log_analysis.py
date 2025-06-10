from fastapi import APIRouter, Depends

from app.api.log_analysis.request.req import LogAnalysisQuery
from app.core.llm.ollama_client import OllamaClient
from app.core.dependencies.mcp import get_mcp_context
from app.core.mcp.mcp_context import MCPContext
from app.core.dependencies.db import get_db

from sqlalchemy.orm import Session

router = APIRouter()


@router.get(
    path='/log-analysis/model',
)
async def log_analysis():
    ollama_client = OllamaClient()
    response = ollama_client.generate('안녕')

    return response


@router.post(
    path='/log-analysis/query'
)
async def query_log_analysis(
        body: LogAnalysisQuery,
        db: Session = Depends(get_db),
        mcp_context: MCPContext = Depends(get_mcp_context)
):
    print(body.user_id)
    print(body.message)

    response = await mcp_context.query(body.message, body.user_id)

    print(response)
    return response