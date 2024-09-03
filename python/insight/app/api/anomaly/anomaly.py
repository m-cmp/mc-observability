from fastapi import APIRouter
from config.ConfigManager import read_config
from app.api.anomaly.response.anomaly_res import ResBodyAnomalyDetectionOptions, AnomalyDetectionOptions


router = APIRouter()


@router.get("/anomaly-detection/options", response_model=ResBodyAnomalyDetectionOptions)
async def get_anomaly_detection_options():
    config_data = read_config("config/anomaly.ini")

    response = ResBodyAnomalyDetectionOptions(data=AnomalyDetectionOptions(**config_data))

    return response
