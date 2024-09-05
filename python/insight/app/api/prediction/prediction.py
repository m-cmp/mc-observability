from fastapi import APIRouter, Depends, Body

from config.ConfigManager import read_config_prediction
from app.api.prediction.request.req import Item, PredictionBody, PredictionPath, GetHistoryPath, GetPredictionHistoryQuery
from app.api.prediction.response.res import ResBodyPredictionOptions, PredictionOptions
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

    response = ResBodyPredictionOptions(
        data=PredictionOptions(**config_data),
        rsCode='200',
        rsMsg='Success'
    )

    return response


@router.post(
    path='/predictions/nsId/{nsId}/target/{targetId}',
    description=post_prediction_description['api_description'],
    responses=post_prediction_description['response']
)
async def predict_monitoring_data(
        body_params: PredictionBody,
        path_params: PredictionPath = Depends()
):
    prediction_service = PredictionService()
    df = prediction_service.get_data(nsId=path_params.nsId, targetId=path_params.targetId, metric_type=body_params.metric_type)
    prediction_result = prediction_service.prediction(df, body_params.prediction_range)
    # print(df)



    return 1


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



