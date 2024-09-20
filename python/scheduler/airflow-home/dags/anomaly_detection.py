from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.hooks.mysql_hook import MySqlHook
from airflow.hooks.base_hook import BaseHook
from datetime import datetime, timedelta
import pandas as pd
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed


default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'retries': 0,
}


def fetch_data_from_db():
    hook = MySqlHook(mysql_conn_id='mcmp_db')
    sql = "SELECT * FROM mc_o11y_insight_anomaly_setting"
    df = hook.get_pandas_df(sql)

    df['LAST_EXECUTION'] = df['LAST_EXECUTION'].astype(str)
    df['REGDATE'] = df['REGDATE'].astype(str)

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

    seq_list = [record['SEQ'] for record in records if record['next_execution_time'] <= current_time]

    return seq_list


def post_to_api(setting_seq):
    conn = BaseHook.get_connection('api_base_url')
    base_url = f"{conn.schema}://{conn.host}:{conn.port}" if conn.port else f"{conn.schema}://{conn.host}"

    url = f"{base_url}/api/o11y/insight/anomaly-detection/{setting_seq}"

    try:
        response = requests.post(url)

        if response.status_code == 200:
            print(f"POST successful for settingSeq {setting_seq}")
        else:
            print(
                f"POST failed for settingSeq {setting_seq}. Status Code: {response.status_code}, Response: {response.text}")
    except Exception as e:
        print(f"Exception during API call for settingSeq {setting_seq}: {str(e)}")


def execute_api_calls(**context):
    seq_list = context['ti'].xcom_pull(task_ids='filter_records')

    if not seq_list:
        print("No records to execute.")
        return

    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = {executor.submit(post_to_api, setting_seq): setting_seq for setting_seq in seq_list}

        for future in as_completed(futures):
            setting_seq = futures[future]
            try:
                future.result()
            except Exception as exc:
                print(f"API call generated an exception for settingSeq {setting_seq}: {exc}")


with DAG(
    dag_id='anomaly_detection',
    default_args=default_args,
    start_date=datetime(2023, 9, 6),
    schedule_interval='*/1 * * * *',
    catchup=False,
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
