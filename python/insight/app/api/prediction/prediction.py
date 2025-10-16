from fastapi import APIRouter, Depends
from config.ConfigManager import ConfigManager
from app.api.prediction.request.req import GetMeasurementPath, PredictionBody, PredictionMCIPath, PredictionVMPath, GetHistoryMCIPath, GetHistoryVMPath, GetPredictionHistoryQuery
from app.api.prediction.response.res import ResBodyPredictionMeasurement, ResBodyPredictionSpecificMeasurement, \
    ResBodyPredictionOptions, PredictionOptions, ResBodyPredictionMCIResult, ResBodyPredictionVMResult, \
    PredictionMCIResult, PredictionVMResult, PredictionMCIHistory, ResBodyPredictionMCIHistory, PredictionVMHistory, ResBodyPredictionVMHistory
from app.api.prediction.description.description import (get_options_description, post_prediction_mci_description, post_prediction_vm_description,
                                                        get_history_mci_description, get_history_vm_description, get_prediction_measurements_description,
                                                        get_specific_measurement_description)
from app.api.prediction.utils.utils import PredictionService
from app.core.dependencies.db import get_db
from sqlalchemy.orm import Session

import logging

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get(
    path='/predictions/measurement',
    description=get_prediction_measurements_description['api_description'],
    responses=get_prediction_measurements_description['response'],
    response_model=ResBodyPredictionMeasurement,
    operation_id="GetPredictionMeasurementList"
)
async def get_prediction_measurements(db: Session = Depends(get_db)):
    config = ConfigManager()
    measurement_field_config = config.get_prediction_config()['measurement_fields']
    prediction_service = PredictionService(db=db)
    result_dict = prediction_service.map_plugin_info(measurement_field_config)

    return ResBodyPredictionMeasurement(data=result_dict)


@router.get(
    path='/predictions/measurement/{measurement}',
    description=get_specific_measurement_description['api_description'],
    responses=get_specific_measurement_description['response'],
    response_model=ResBodyPredictionSpecificMeasurement,
    operation_id="GetPredictionFieldListByMeasurement"
)
async def get_specific_measurement(
        path_params: GetMeasurementPath = Depends(),
        db: Session = Depends(get_db)
):
    config = ConfigManager()
    measurement_field_config = config.get_prediction_config()['measurement_fields']
    prediction_service = PredictionService(db=db)
    result_dict = prediction_service.map_plugin_info(measurement_field_config, target_measurement=path_params)

    return ResBodyPredictionSpecificMeasurement(data=result_dict)


@router.get(
    path='/predictions/options',
    description=get_options_description['api_description'],
    responses=get_options_description['response'],
    response_model=ResBodyPredictionOptions,
    operation_id="GetPredictionOptions"
)
async def get_prediction_options():
    config = ConfigManager()
    config_data = config.get_prediction_config()

    return ResBodyPredictionOptions(data=PredictionOptions(**config_data))

@router.post(
    path='/predictions/ns/{nsId}/mci/{mciId}',
    description=post_prediction_mci_description['api_description'],
    responses=post_prediction_mci_description['response'],
    response_model=ResBodyPredictionMCIResult,
    operation_id='PostPredictionMCI'
)
async def predict_mci(
        body_params: PredictionBody,
        path_params: PredictionMCIPath = Depends()
):
    prediction_service = PredictionService()
    df = prediction_service.get_data(path_params, body_params)
    result_dict = prediction_service.predict(df, path_params, body_params)
    prediction_result = PredictionMCIResult(
        ns_id=path_params.nsId,
        mci_id=path_params.mciId,
        measurement=body_params.measurement,
        values=result_dict
    )

    return ResBodyPredictionMCIResult(data=prediction_result)

@router.post(
    path='/predictions/ns/{nsId}/mci/{mciId}/vm/{vmId}',
    description=post_prediction_vm_description['api_description'],
    responses=post_prediction_vm_description['response'],
    response_model=ResBodyPredictionVMResult,
    operation_id='PostPredictionVM'
)
async def predict_vm(
        body_params: PredictionBody,
        path_params: PredictionVMPath = Depends()
):
    prediction_service = PredictionService()
    df = prediction_service.get_data(path_params, body_params)
    result_dict = prediction_service.predict(df, path_params, body_params)
    prediction_result = PredictionVMResult(
        ns_id=path_params.nsId,
        mci_id=path_params.mciId,
        vm_id=path_params.vmId,
        measurement=body_params.measurement,
        values=result_dict
    )

    return ResBodyPredictionVMResult(data=prediction_result)


@router.get(
    path='/predictions/ns/{nsId}/mci/{mciId}/history',
    description=get_history_mci_description['api_description'],
    responses=get_history_mci_description['response'],
    response_model=ResBodyPredictionMCIHistory,
    operation_id="GetPredictionMCIHistory"
)
async def get_prediction_mci_history(
        path_params: GetHistoryMCIPath = Depends(),
        query_params: GetPredictionHistoryQuery = Depends()
):
    prediction_service = PredictionService()
    result_dict = prediction_service.get_prediction_history(path_params, query_params)

    prediction_history = PredictionMCIHistory(
        ns_id=path_params.nsId,
        mci_id=path_params.mciId,
        measurement=query_params.measurement,
        values=result_dict
    )

    return ResBodyPredictionMCIHistory(data=prediction_history)

@router.get(
    path='/predictions/ns/{nsId}/mci/{mciId}/vm/{vmId}/history',
    description=get_history_vm_description['api_description'],
    responses=get_history_vm_description['response'],
    response_model=ResBodyPredictionVMHistory,
    operation_id="GetPredictionVMHistory"
)
async def get_prediction_vm_history(
        path_params: GetHistoryVMPath = Depends(),
        query_params: GetPredictionHistoryQuery = Depends()
):
    prediction_service = PredictionService()
    result_dict = prediction_service.get_prediction_history(path_params, query_params)

    prediction_history = PredictionVMHistory(
        ns_id=path_params.nsId,
        mci_id=path_params.mciId,
        vm_id=path_params.vmId,
        measurement=query_params.measurement,
        values=result_dict
    )

    return ResBodyPredictionVMHistory(data=prediction_history)
