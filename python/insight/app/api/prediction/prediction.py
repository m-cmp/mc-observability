from fastapi import APIRouter, Depends, Path, Query

from config.ConfigManager import read_config_prediction
from app.api.prediction.request.req import GetHistoryPathParams, MetricType, GetPredictionHistoryFilter
from app.api.prediction.response.res import ResBodyPredictionOptions, PredictionOptions
from app.api.prediction.description.description import get_options_example, post_prediction_example, get_history_example


router = APIRouter()


@router.get('/predictions/options', response_model=ResBodyPredictionOptions, responses=get_options_example['responses'])
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


@router.post('/predictions/nsId/{nsId}/target/{targetId}', responses=post_prediction_example['response'])
async def predict_monitoring_data(nsId: str, targetId: str):


    return 1


@router.get('/predictions/nsId/{nsId}/target/{targetId}/history', responses=get_history_example['response'])
async def get_prediction_history(
        path_params: GetHistoryPathParams = Depends(),
        query_params: GetPredictionHistoryFilter = Depends()
):
    """
    Get previously stored prediction data for a specific VM or MCI group.
    """

    """
    @path_params: 
        nsId
        targetId
        
    @query_params: 
        metric_type
        start_time
        end_time
        
    :return:
    """

    print(f'nsId: {path_params.nsId}')
    print(f'targetId: {path_params.targetId}')
    print(f'query params: {query_params}')

    pass