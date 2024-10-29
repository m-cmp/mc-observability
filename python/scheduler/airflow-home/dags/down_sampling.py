from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.hooks.base_hook import BaseHook
from airflow.models import Variable
from datetime import datetime
from downsampling_utils.multi import DataProcessor
import requests
import pendulum


local_tz = pendulum.timezone("Asia/Seoul")

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'retries': 0,
}

api_base_url = Variable.get('API_BASE_URL')


def data_down_sampling():
    api_url = api_base_url + f'/monitoring/influxdb/measurement'
    response = requests.get(api_url)
    response.raise_for_status()

    response_data = response.json()
    metric_info_list = response_data.get('data', [])

    influxdb = BaseHook.get_connection('influxdb')

    data_processor = DataProcessor(api_base_url=api_base_url, influxdb=influxdb, metric_info_list=metric_info_list)
    data_processor.process_measurements()


with DAG(
        dag_id='down_sampling',
        default_args=default_args,
        description='A DAG to down-sample data',
        start_date=datetime(2024, 10, 28, tzinfo=local_tz),
        schedule_interval='0 * * * *',
        catchup=False,
) as dag:
    data_down_sampling = PythonOperator(
        task_id='data_down_sampling',
        python_callable=data_down_sampling,
    )
