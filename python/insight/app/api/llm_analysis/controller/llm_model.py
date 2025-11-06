from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.llm_analysis.description.llm_model import get_llm_model_options_description
from app.api.llm_analysis.response.res import ResBodyLLMModel
from app.api.llm_analysis.utils.llm_model import CommonModelService
from app.core.dependencies.db import get_db
from config.ConfigManager import ConfigManager

router = APIRouter()


@router.get(
    path="/llm/model",
    description=get_llm_model_options_description["api_description"],
    responses=get_llm_model_options_description["response"],
    response_model=ResBodyLLMModel,
    operation_id="GetLLMModelOptions",
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
