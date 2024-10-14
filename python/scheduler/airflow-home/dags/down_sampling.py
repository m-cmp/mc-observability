from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.hooks.base_hook import BaseHook
from airflow.models import Variable
from datetime import datetime, timedelta
from downsampling_utils.multi import DataProcessor
import requests
import pendulum
import pytz

local_tz = pendulum.timezone("Asia/Seoul")

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'retries': 0,
}

api_base_url = Variable.get('API_BASE_URL')


def get_influxdb_seq(ti):
    try:
        api_url = f"{api_base_url}/monitoring/influxdb"
        response = requests.get(api_url)
        response.raise_for_status()

        response_data = response.json()

        influxdb_list = response_data.get('data', [])
        seq_list = [entry['seq'] for entry in influxdb_list if 'seq' in entry]

        ti.xcom_push(key='influxdb_seq_list', value=seq_list)

    except requests.exceptions.RequestException as e:
        print(f"Failed to fetch InfluxDB list: {e}")
        raise


def data_down_sampling(ti, **context):
    exec_date = context['data_interval_start'] + timedelta(hours=1)
    exec_date_utc = exec_date.astimezone(pytz.utc)
    exec_time_str = exec_date_utc.strftime('%Y-%m-%dT%H:%M:%SZ')

    seq_list = ti.xcom_pull(key='influxdb_seq_list', task_ids='call_influxdb_seq')

    # api_url = api_base_url + f'/monitoring/influxdb/{seq_list[0]}/measurement'
    api_url = api_base_url + f'/monitoring/influxdb/measurement'
    response = requests.get(api_url)
    response.raise_for_status()

    response_data = response.json()
    metric_info_list = response_data.get('data', [])
    # metric_info_list = [{'measurement': 'cpu', 'fields': [{'key':'cpu', 'type':'string'}]}]

    influxdb = BaseHook.get_connection('influxdb')

    data_processor = DataProcessor(api_base_url=api_base_url, influxdb=influxdb, influx_seq=seq_list,
                                   metric_info_list=metric_info_list, end=exec_time_str)
    data_processor.process_measurements()


with DAG(
        dag_id='down_sampling',
        default_args=default_args,
        description='A DAG to down-sample data',
        start_date=datetime(2024, 9, 12, tzinfo=local_tz),
        schedule_interval='0 * * * *',
        catchup=False,
) as dag:
    call_influxdb_seq = PythonOperator(
        task_id='call_influxdb_seq',
        python_callable=get_influxdb_seq,
    )

    data_down_sampling = PythonOperator(
        task_id='data_down_sampling',
        python_callable=data_down_sampling,
    )

    call_influxdb_seq >> data_down_sampling

if __name__ == "__main__":
    dag.cli()
