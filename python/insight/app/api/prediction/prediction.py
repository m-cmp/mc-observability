from fastapi import APIRouter, Depends, Body

from config.ConfigManager import read_config_prediction
from app.api.prediction.request.req import Item, PredictionBody, PredictionPath, GetHistoryPath, GetPredictionHistoryQuery
from app.api.prediction.response.res import ResBodyPredictionOptions, PredictionOptions, ResBodyPredictionResult, \
    PredictionResult
from app.api.prediction.description.description import get_options_description, post_prediction_description, get_history_description
from app.api.prediction.utils.utils import PredictionService
from app.common.database.InfluxDB.influxdb_connection import read_influxdb_config

import pandas as pd

router = APIRouter()


@router.get(
    path='/predictions/options',
    description=get_options_description['api_description'],
    responses=get_options_description['responses']
)
async def get_prediction_options():
    read_influxdb_config()
    config_data = read_config_prediction('config/prediction.ini')

    return ResBodyPredictionOptions(data=PredictionOptions(**config_data))


@router.post(
    path='/predictions/nsId/{nsId}/target/{targetId}',
    description=post_prediction_description['api_description'],
    responses=post_prediction_description['response']
)
async def predict_monitoring_data(
        body_params: PredictionBody,
        path_params: PredictionPath = Depends()
):
    prediction_service = PredictionService(
        nsId=path_params.nsId,
        targetId=path_params.targetId,
        metric_type=body_params.metric_type
    )
    df = prediction_service.get_data()
    result_dict = prediction_service.predict(df, body_params.prediction_range)

    prediction_result = PredictionResult(
        nsId=path_params.nsId,
        targetId=path_params.targetId,
        metric_type=body_params.metric_type,
        target_type=body_params.target_type,
        values=result_dict
    )

    return ResBodyPredictionResult(data=prediction_result)


@router.get(
    path='/predictions/nsId/{nsId}/target/{targetId}/history',
    description=get_history_description['api_description'],
    responses=get_history_description['response']
)
async def get_prediction_history(
        path_params: GetHistoryPath = Depends(),
        query_params: GetPredictionHistoryQuery = Depends()
):
    print(f'nsId: {path_params.nsId}')
    print(f'targetId: {path_params.targetId}')
    print(f'query params: {query_params}')

    pass



