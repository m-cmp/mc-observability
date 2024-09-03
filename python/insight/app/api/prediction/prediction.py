from fastapi import APIRouter

from config.ConfigManager import read_config_prediction
from app.api.prediction.response.prediction_res import ResBodyPredictionOptions, PredictionOptions


router = APIRouter()


@router.get('/predictions/options', response_model=ResBodyPredictionOptions)
async def get_prediction_options():
    """
    Fetch the available target types, metric types, and prediction range options for the prediction API.
    """

    config_data = read_config_prediction('config/prediction.ini')

    response = ResBodyPredictionOptions(
        data=PredictionOptions(**config_data),
        rsCode='200',
        rsMsg='Success'
    )

    return response

