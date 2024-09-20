from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.api.anomaly.response.res import (ResBodyAnomalyDetectionOptions, AnomalyDetectionOptions,
                                          ResBodyAnomalyDetectionSettings)
from app.api.anomaly.utils.utils import AnomalyService, get_db
from app.api.anomaly.utils.history import AnomalyHistoryService
from app.api.anomaly.utils.setting import AnomalySettingsService
from app.api.anomaly.response.res import ResBodyVoid, ResBodyAnomalyDetectionHistoryResponse
from app.api.anomaly.request.req import (AnomalyDetectionTargetRegistration, AnomalyDetectionTargetUpdate,
                                         GetHistoryPathParams, GetAnomalyHistoryFilter)
from config.ConfigManager import ConfigManager

router = APIRouter()


@router.get("/anomaly-detection/options", response_model=ResBodyAnomalyDetectionOptions)
async def get_available_options_for_anomaly_detection():
    """
    Fetch the available target types, metric types, and interval options for the anomaly detection API.
    """
    config=ConfigManager()
    config_data = config.get_anomaly_config()

    response = ResBodyAnomalyDetectionOptions(data=AnomalyDetectionOptions(**config_data))

    return response


@router.get("/anomaly-detection/settings", response_model=ResBodyAnomalyDetectionSettings)
async def get_all_anomaly_detection_settings(db: Session = Depends(get_db)):
    """
    Fetch the current settings for all anomaly detection targets.
    """
    anomaly_setting_service = AnomalySettingsService(db=db)
    response = anomaly_setting_service.get_all_settings()
    return response


@router.post("/anomaly-detection/settings", response_model=ResBodyVoid)
async def register_anomaly_detection_target(body: AnomalyDetectionTargetRegistration, db: Session = Depends(get_db)):
    """
    Register a target for anomaly detection and automatically schedule detection tasks.
    """
    service = AnomalySettingsService(db=db)
    return service.create_setting(setting_data=body.dict())


@router.put("/anomaly-detection/settings/{settingSeq}", response_model=ResBodyVoid)
async def update_anomaly_detection_target(settingSeq: int, body: AnomalyDetectionTargetUpdate, db: Session = Depends(get_db)):
    """
    Modify the settings for a specific anomaly detection target, including the monitoring metric and interval.
    """
    service = AnomalySettingsService(db=db)
    return service.update_setting(setting_seq=settingSeq, update_data=body.dict())


@router.delete("/anomaly-detection/settings/{settingSeq}", response_model=ResBodyVoid)
async def delete_anomaly_detection_target(settingSeq: int, db: Session = Depends(get_db)):
    """
    Remove a target from anomaly detection, stopping and removing any scheduled tasks.
    """
    service = AnomalySettingsService(db=db)
    return service.delete_setting(setting_seq=settingSeq)


@router.get("/anomaly-detection/settings/nsId/{nsId}/target/{targetId}", response_model=ResBodyAnomalyDetectionSettings)
async def get_specific_anomaly_detection_target(nsId: str, targetId: str, db: Session = Depends(get_db)):
    """
    Fetch the current settings for a specific anomaly detection target.
    """
    service = AnomalySettingsService(db=db)
    return service.get_setting(ns_id=nsId, target_id=targetId)


@router.get("/anomaly-detection/nsId/{nsId}/target/{targetId}/history",
            response_model=ResBodyAnomalyDetectionHistoryResponse)
async def get_anomaly_detection_history(
        path_params: GetHistoryPathParams = Depends(),
        query_params: GetAnomalyHistoryFilter = Depends(),
):
    """
    Fetch the results of anomaly detection for a specific target within a given time range.
    """
    service = AnomalyHistoryService(path_params=path_params, query_params=query_params)
    data = service.get_anomaly_detection_results()
    return ResBodyAnomalyDetectionHistoryResponse(data=data)


@router.post("/anomaly-detection/{settingSeq}", response_model=ResBodyVoid)
async def post_anomaly_detection(settingSeq: int, db: Session = Depends(get_db)):
    """
    Request anomaly detection
    """
    service = AnomalyService(db=db, seq=settingSeq)
    service.anomaly_detection()

    return ResBodyVoid(rsMsg="Anomaly Detection Success")
