from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.api.anomaly.description.description import (get_options_description, get_settings_description,
                                                     post_settings_description, put_settings_description,
                                                     delete_settings_description, get_specific_settings_description,
                                                     get_history_description, post_anomaly_detection_description)
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


@router.get(
    path="/anomaly-detection/options",
    description=get_options_description['api_description'],
    responses=get_options_description['response'],
    response_model=ResBodyAnomalyDetectionOptions,
    operation_id="GetAnomalyDetectionOptions",
)
async def get_available_options_for_anomaly_detection():
    config = ConfigManager()
    config_data = config.get_anomaly_config()

    response = ResBodyAnomalyDetectionOptions(data=AnomalyDetectionOptions(**config_data))

    return response


@router.get(
    path="/anomaly-detection/settings",
    description=get_settings_description['api_description'],
    response_model=ResBodyAnomalyDetectionSettings,
    operation_id="GetAllAnomalyDetectionSettings"
)
async def get_all_anomaly_detection_settings(db: Session = Depends(get_db)):
    anomaly_setting_service = AnomalySettingsService(db=db)
    response = anomaly_setting_service.get_all_settings()
    return response


@router.post(
    path="/anomaly-detection/settings",
    description=post_settings_description['api_description'],
    response_model=ResBodyVoid,
    operation_id="PostAnomalyDetectionSettings"
)
async def register_anomaly_detection_target(body: AnomalyDetectionTargetRegistration, db: Session = Depends(get_db)):
    service = AnomalySettingsService(db=db)
    return service.create_setting(setting_data=body.dict())


@router.put(
    path="/anomaly-detection/settings/{settingSeq}",
    description=put_settings_description['api_description'],
    response_model=ResBodyVoid,
    operation_id="PutAnomalyDetectionSettings"
)
async def update_anomaly_detection_target(settingSeq: int, body: AnomalyDetectionTargetUpdate, db: Session = Depends(get_db)):
    service = AnomalySettingsService(db=db)
    return service.update_setting(setting_seq=settingSeq, update_data=body.dict())


@router.delete(
    path="/anomaly-detection/settings/{settingSeq}",
    description=delete_settings_description['api_description'],
    response_model=ResBodyVoid,
    operation_id="DeleteAnomalyDetectionSettings"
)
async def delete_anomaly_detection_target(settingSeq: int, db: Session = Depends(get_db)):
    service = AnomalySettingsService(db=db)
    return service.delete_setting(setting_seq=settingSeq)


@router.get(
    path="/anomaly-detection/settings/nsId/{nsId}/target/{targetId}",
    description=get_specific_settings_description['api_description'],
    response_model=ResBodyAnomalyDetectionSettings,
    operation_id="GetTargetAnomalyDetectionSettings"
)
async def get_specific_anomaly_detection_target(nsId: str, targetId: str, db: Session = Depends(get_db)):
    service = AnomalySettingsService(db=db)
    return service.get_setting(ns_id=nsId, target_id=targetId)


@router.get(
    path="/anomaly-detection/nsId/{nsId}/target/{targetId}/history",
    description=get_history_description['api_description'],
    response_model=ResBodyAnomalyDetectionHistoryResponse,
    operation_id="GetAnomalyDetectionHistory"
)
async def get_anomaly_detection_history(
        path_params: GetHistoryPathParams = Depends(),
        query_params: GetAnomalyHistoryFilter = Depends(),
):
    service = AnomalyHistoryService(path_params=path_params, query_params=query_params)
    data = service.get_anomaly_detection_results()
    return ResBodyAnomalyDetectionHistoryResponse(data=data)


@router.post(
    path="/anomaly-detection/{settingSeq}",
    description=post_anomaly_detection_description['api_description'],
    response_model=ResBodyVoid,
    operation_id="PostAnomalyDetection"
)
async def post_anomaly_detection(settingSeq: int, db: Session = Depends(get_db)):
    service = AnomalyService(db=db, seq=settingSeq)
    service.anomaly_detection()

    return ResBodyVoid(rsMsg="Anomaly Detection Success")
