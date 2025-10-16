from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.api.anomaly.description.description import (get_options_description, get_settings_description,
                                                     post_settings_description, put_settings_description,
                                                     delete_settings_description, get_specific_settings_mci_description, get_specific_settings_vm_description,
                                                     get_history_mci_description, get_history_vm_description, post_anomaly_detection_description,
                                                     get_anomaly_detection_measurements_description,
                                                     get_specific_measurement_description)
from app.api.anomaly.response.res import (ResBodyAnomalyDetectionOptions, AnomalyDetectionOptions,
                                          ResBodyAnomalyDetectionSettings)
from app.api.anomaly.utils.utils import AnomalyService
from app.api.anomaly.utils.history import AnomalyHistoryService
from app.api.anomaly.utils.setting import AnomalySettingsService
from app.api.anomaly.response.res import (ResBodyAnomalyDetectionMeasurement, ResBodyAnomalyDetectionSpecificMeasurement,
                                          ResBodyVoid, ResBodyAnomalyDetectionHistoryResponse)
from app.api.anomaly.request.req import (GetMeasurementPath, AnomalyDetectionTargetRegistration, AnomalyDetectionTargetUpdate,
                                         GetHistoryMCIPath, GetHistoryVMPath, GetAnomalyHistoryFilter)
from config.ConfigManager import ConfigManager
from app.core.dependencies.db import get_db
from fastapi.responses import JSONResponse

router = APIRouter()


@router.get(
    path='/anomaly-detection/measurement',
    description=get_anomaly_detection_measurements_description['api_description'],
    responses=get_anomaly_detection_measurements_description['response'],
    response_model=ResBodyAnomalyDetectionMeasurement,
    operation_id="GetAnomalyMeasurementList"
)
async def get_anomaly_detection_measurements(
        db: Session = Depends(get_db)
):
    config = ConfigManager()
    measurement_field_config = config.get_anomaly_config()['measurement_fields']
    anomaly_setting_service = AnomalySettingsService(db=db)
    result_dict = anomaly_setting_service.map_plugin_info(measurement_field_config)

    return ResBodyAnomalyDetectionMeasurement(data=result_dict)


@router.get(
    path='/anomaly-detection/measurement/{measurement}',
    description=get_specific_measurement_description['api_description'],
    responses=get_specific_measurement_description['response'],
    response_model=ResBodyAnomalyDetectionSpecificMeasurement,
    operation_id="GetAnomalyFieldListByMeasurement"
)
async def get_specific_measurement(
        path_params: GetMeasurementPath = Depends(),
        db: Session = Depends(get_db)
):
    config = ConfigManager()
    measurement_field_config = config.get_anomaly_config()['measurement_fields']
    anomaly_setting_service = AnomalySettingsService(db=db)
    result_dict = anomaly_setting_service.map_plugin_info(measurement_field_config, target_measurement=path_params)

    return ResBodyAnomalyDetectionSpecificMeasurement(data=result_dict)


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
async def get_all_anomaly_detection_settings(
        db: Session = Depends(get_db)
):
    anomaly_setting_service = AnomalySettingsService(db=db)
    response = anomaly_setting_service.get_all_settings()
    return response


@router.post(
    path="/anomaly-detection/settings",
    description=post_settings_description['api_description'],
    response_model=ResBodyVoid,
    operation_id="PostAnomalyDetectionSettings"
)
async def register_anomaly_detection_target(
        body: AnomalyDetectionTargetRegistration,
        db: Session = Depends(get_db)
):
    service = AnomalySettingsService(db=db)
    return service.create_setting(setting_data=body.dict())


@router.put(
    path="/anomaly-detection/settings/{settingSeq}",
    description=put_settings_description['api_description'],
    response_model=ResBodyVoid,
    operation_id="PutAnomalyDetectionSettings"
)
async def update_anomaly_detection_target(
        settingSeq: int,
        body: AnomalyDetectionTargetUpdate,
        db: Session = Depends(get_db)
):
    service = AnomalySettingsService(db=db)
    return service.update_setting(setting_seq=settingSeq, update_data=body.dict())


@router.delete(
    path="/anomaly-detection/settings/{settingSeq}",
    description=delete_settings_description['api_description'],
    response_model=ResBodyVoid,
    operation_id="DeleteAnomalyDetectionSettings"
)
async def delete_anomaly_detection_target(
        settingSeq: int,
        db: Session = Depends(get_db)
):
    service = AnomalySettingsService(db=db)
    return service.delete_setting(setting_seq=settingSeq)


@router.get(
    path="/anomaly-detection/settings/ns/{nsId}/mci/{mciId}",
    description=get_specific_settings_mci_description['api_description'],
    response_model=ResBodyAnomalyDetectionSettings,
    operation_id="GetMCIAnomalyDetectionSettings"
)
async def get_specific_anomaly_detection_mci(
        nsId: str,
        mciId: str,
        db: Session = Depends(get_db)
):
    service = AnomalySettingsService(db=db)
    return service.get_setting(ns_id=nsId, mci_id=mciId)

@router.get(
    path="/anomaly-detection/settings/ns/{nsId}/mci/{mciId}/vm/{vmId}",
    description=get_specific_settings_vm_description['api_description'],
    response_model=ResBodyAnomalyDetectionSettings,
    operation_id="GetVMAnomalyDetectionSettings"
)
async def get_specific_anomaly_detection_vm(
        nsId: str,
        mciId: str,
        vmId: str,
        db: Session = Depends(get_db)
):
    service = AnomalySettingsService(db=db)
    return service.get_setting(ns_id=nsId, mci_id=mciId, vm_id=vmId)

@router.get(
    path="/anomaly-detection/ns/{nsId}/mci/{mciId}/history",
    description=get_history_mci_description['api_description'],
    response_model=ResBodyAnomalyDetectionHistoryResponse,
    operation_id="GetAnomalyDetectionMCIHistory"
)
async def get_anomaly_detection_mci_history(
        path_params: GetHistoryMCIPath = Depends(),
        query_params: GetAnomalyHistoryFilter = Depends(),
):
    service = AnomalyHistoryService(path_params=path_params, query_params=query_params)
    data = service.get_anomaly_detection_results()
    return ResBodyAnomalyDetectionHistoryResponse(data=data)


@router.get(
    path="/anomaly-detection/ns/{nsId}/mci/{mciId}/vm/{vmId}/history",
    description=get_history_vm_description['api_description'],
    response_model=ResBodyAnomalyDetectionHistoryResponse,
    operation_id="GetAnomalyDetectionVMHistory"
)
async def get_anomaly_detection_vm_history(
        path_params: GetHistoryVMPath = Depends(),
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
async def post_anomaly_detection(
        settingSeq: int,
        db: Session = Depends(get_db)
):
    service = AnomalyService(db=db, seq=settingSeq)
    service.anomaly_detection()

    return ResBodyVoid(rs_msg="Anomaly Detection Success")
