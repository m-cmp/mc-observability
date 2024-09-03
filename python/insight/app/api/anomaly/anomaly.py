from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.api.anomaly.response.res import (ResBodyAnomalyDetectionOptions, AnomalyDetectionOptions
, ResBodyAnomalyDetectionSettings)
from app.api.anomaly.utils.utils import AnomalySettings, get_db
from config.ConfigManager import read_config

router = APIRouter()


@router.get("/anomaly-detection/options", response_model=ResBodyAnomalyDetectionOptions)
async def get_available_options_for_anomaly_detection():
    config_data = read_config("config/anomaly.ini")

    response = ResBodyAnomalyDetectionOptions(data=AnomalyDetectionOptions(**config_data))

    return response


@router.get("/anomaly-detection/settings", response_model=ResBodyAnomalyDetectionSettings)
async def get_all_anomaly_detection_settings(db: Session = Depends(get_db)):
    anomaly_setting_service = AnomalySettings(db=db)
    response = anomaly_setting_service.get_all_settings()
    return response
