from fastapi import APIRouter, Depends
from config.ConfigManager import ConfigManager
from app.api.prediction.request.req import GetMeasurementPath, PredictionBody, PredictionPath, GetHistoryPath, GetPredictionHistoryQuery
from app.api.prediction.response.res import ResBodyPredictionMeasurement, ResBodyPredictionSpecificMeasurement, \
    ResBodyPredictionOptions, PredictionOptions, ResBodyPredictionResult, \
    PredictionResult, PredictionHistory, ResBodyPredictionHistory
from app.api.prediction.description.description import (get_options_description, post_prediction_description,
                                                        get_history_description, get_prediction_measurements_description,
                                                        get_specific_measurement_description)
from app.api.prediction.utils.utils import PredictionService
from app.api.anomaly.utils.utils import get_db
from sqlalchemy.orm import Session

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
    operation_id="GetPredictionFieldListByMesurement"
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
    path='/predictions/nsId/{nsId}/target/{targetId}',
    description=post_prediction_description['api_description'],
    responses=post_prediction_description['response'],
    response_model=ResBodyPredictionResult,
    operation_id="PostPrediction"
)
async def predict_monitoring_data(
        body_params: PredictionBody,
        path_params: PredictionPath = Depends()
):
    prediction_service = PredictionService()
    df = prediction_service.get_data(path_params, body_params)
    result_dict = prediction_service.predict(df, path_params, body_params)

    prediction_result = PredictionResult(
        ns_id=path_params.nsId,
        target_id=path_params.targetId,
        measurement=body_params.measurement,
        target_type=body_params.target_type,
        values=result_dict
    )

    return ResBodyPredictionResult(data=prediction_result)


@router.get(
    path='/predictions/nsId/{nsId}/target/{targetId}/history',
    description=get_history_description['api_description'],
    responses=get_history_description['response'],
    response_model=ResBodyPredictionHistory,
    operation_id="GetPredictionHistory"
)
async def get_prediction_history(
        path_params: GetHistoryPath = Depends(),
        query_params: GetPredictionHistoryQuery = Depends()
):
    prediction_service = PredictionService()
    result_dict = prediction_service.get_prediction_history(path_params, query_params)

    prediction_history = PredictionHistory(
        ns_id=path_params.nsId,
        target_id=path_params.targetId,
        measurement=query_params.measurement,
        values=result_dict
    )

    return ResBodyPredictionHistory(data=prediction_history)


