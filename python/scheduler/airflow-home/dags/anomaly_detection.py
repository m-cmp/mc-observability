from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.hooks.mysql_hook import MySqlHook
from datetime import datetime, timedelta
import pandas as pd
import requests


default_args = {
    'owner': 'airflow',
    'retries': 0,
}


def fetch_data_from_db():
    hook = MySqlHook(mysql_conn_id='mcmp_db')
    sql = "SELECT * FROM anomaly_detection_settings"
    df = hook.get_pandas_df(sql)
    return df.to_dict('records')


def parse_execution_intervals(record):
    interval_str = record['EXECUTION_INTERVAL']
    time_value = int(interval_str[:-1])
    unit = interval_str[-1]

    if unit == 'm':
        return timedelta(minutes=time_value)
    elif unit == 'h':
        return timedelta(hours=time_value)
    elif unit == 's':
        return timedelta(seconds=time_value)
    else:
        raise ValueError(f"Unknown time unit: {unit}")


def filter_records_to_execute(records):
    current_time = datetime.now()

    for record in records:
        record['LAST_EXECUTION'] = pd.to_datetime(record['LAST_EXECUTION'])
        interval = parse_execution_intervals(record)
        record['next_execution_time'] = record['LAST_EXECUTION'] + interval

    rows_to_execute = [record for record in records if record['next_execution_time'] <= current_time]
    return rows_to_execute


def post_to_api(record):
    setting_seq = record['SEQ']
    url = f'http://127.0.0.1:9001/api/o11y/insight/anomaly-detection/{setting_seq}'

    response = requests.post(url)

    if response.status_code == 200:
        print(f"POST successful for settingSeq {setting_seq}")
    else:
        print(
            f"POST failed for settingSeq {setting_seq}. Status Code: {response.status_code}, Response: {response.text}")


def execute_api_calls(**context):
    rows_to_execute = context['ti'].xcom_pull(task_ids='filter_records')

    for record in rows_to_execute:
        post_to_api(record)


with DAG(
    dag_id='anomaly_detection',
    default_args=default_args,
    start_date=datetime(2023, 9, 6),
    schedule_interval='*/1 * * * *',
) as dag:

    fetch_data_task = PythonOperator(
        task_id='fetch_data',
        python_callable=fetch_data_from_db
    )

    filter_records_task = PythonOperator(
        task_id='filter_records',
        python_callable=lambda **kwargs: filter_records_to_execute(kwargs['ti'].xcom_pull(task_ids='fetch_data')),
        provide_context=True
    )

    execute_api_calls_task = PythonOperator(
        task_id='execute_api_calls',
        python_callable=execute_api_calls,
        provide_context=True
    )

    fetch_data_task >> filter_records_task >> execute_api_calls_task
