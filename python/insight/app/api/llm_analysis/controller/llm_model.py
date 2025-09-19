from fastapi import APIRouter, Depends
from app.api.llm_analysis.response.res import ResBodyLLMModel
from app.api.llm_analysis.utils.llm_model import CommonModelService
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session
from config.ConfigManager import ConfigManager

router = APIRouter()


@router.get(
    path="/llm/model",
    description="Get model options for LLM analysis",
    response_model=ResBodyLLMModel,
    operation_id="GetLLMAModelOptions",
)
async def get_llm_model_options(db: Session = Depends(get_db)):
    """
    Get model options for both log and alarm analysis.
    """
    config = ConfigManager()
    model_info_config = config.get_llm_model_config()

    model_service = CommonModelService(db=db)
    result = model_service.get_model_list(model_info_config)

    return ResBodyLLMModel(data=result)
