from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.hooks.mysql_hook import MySqlHook
from airflow.hooks.base_hook import BaseHook
from datetime import datetime, timedelta
import pandas as pd
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
import logging
import os

# ---------- Debug settings ----------
DEBUG_VERBOSE_HTTP = os.environ.get("ANOMALY_DEBUG_HTTP", "0") == "1"
LOGGER = logging.getLogger("anomaly_detection")
if not LOGGER.handlers:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
LOGGER.setLevel(logging.DEBUG)

# Very verbose HTTP wire logs (원하면 export ANOMALY_DEBUG_HTTP=1)
if DEBUG_VERBOSE_HTTP:
    try:
        import http.client as http_client
        http_client.HTTPConnection.debuglevel = 1
        logging.getLogger("requests.packages.urllib3").setLevel(logging.DEBUG)
        logging.getLogger("urllib3").setLevel(logging.DEBUG)
        logging.getLogger("requests.packages.urllib3").propagate = True
        logging.getLogger("urllib3").propagate = True
        LOGGER.info("Enabled verbose HTTP wire logging")
    except Exception as _e:
        LOGGER.warning(f"Failed to enable HTTP wire logging: {_e}")

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'retries': 0,
}

def fetch_data_from_db():
    LOGGER.info("[fetch_data_from_db] Using connection id = mcmp_db")
    hook = MySqlHook(mysql_conn_id='mcmp_db')
    sql = "SELECT * FROM mc_o11y_insight_anomaly_setting"
    LOGGER.debug(f"[fetch_data_from_db] SQL => {sql}")
    df = hook.get_pandas_df(sql)

    # 원래 로직 유지
    df['LAST_EXECUTION'] = df['LAST_EXECUTION'].astype(str)
    df['REGDATE'] = df['REGDATE'].astype(str)

    # Debug summary
    LOGGER.info(f"[fetch_data_from_db] Loaded rows: {len(df)}")
    if not df.empty:
        LOGGER.debug(f"[fetch_data_from_db] Columns: {list(df.columns)}")
        LOGGER.debug(f"[fetch_data_from_db] Head(3):\n{df.head(3)}")

    return df.to_dict('records')

def parse_execution_intervals(record):
    interval_str = record['EXECUTION_INTERVAL']
    time_value = int(interval_str[:-1])
    unit = interval_str[-1]
    LOGGER.debug(f"[parse_execution_intervals] SEQ={record.get('SEQ')} raw='{interval_str}' -> value={time_value}, unit='{unit}'")

    if unit == 'm':
        return timedelta(minutes=time_value)
    elif unit == 'h':
        return timedelta(hours=time_value)
    elif unit == 's':
        return timedelta(seconds=time_value)
    else:
        raise ValueError(f"Unknown time unit: {unit}")

def filter_records_to_execute(records):
    current_time = datetime.utcnow()
    LOGGER.info(f"[filter_records_to_execute] now(UTC)={current_time}")

    debug_rows = []
    for record in records:
        last_execution = record['LAST_EXECUTION']
        raw_before = last_execution
        if last_execution == 'None' or last_execution == 'NaT':
            record['LAST_EXECUTION'] = pd.Timestamp('1970-01-01')
        else:
            record['LAST_EXECUTION'] = pd.to_datetime(last_execution)

        interval = parse_execution_intervals(record)
        record['next_execution_time'] = record['LAST_EXECUTION'] + interval

        due = record['next_execution_time'] <= current_time
        debug_rows.append({
            "SEQ": record.get('SEQ'),
            "LAST_EXECUTION_raw": raw_before,
            "LAST_EXECUTION_parsed": str(record['LAST_EXECUTION']),
            "interval": str(interval),
            "next_execution_time": str(record['next_execution_time']),
            "now": str(current_time),
            "due": due
        })

    # 상세 로그
    if debug_rows:
        LOGGER.info("[filter_records_to_execute] Computed execution windows:")
        for r in debug_rows:
            LOGGER.info(f"  SEQ={r['SEQ']} | LAST_EXECUTION(raw={r['LAST_EXECUTION_raw']}, parsed={r['LAST_EXECUTION_parsed']}) "
                        f"| interval={r['interval']} | next={r['next_execution_time']} | now={r['now']} | due={r['due']}")

    seq_list = [record['SEQ'] for record in records if record['next_execution_time'] <= current_time]
    LOGGER.info(f"[filter_records_to_execute] due_count={len(seq_list)} seq_list={seq_list}")

    return seq_list

def post_to_api(setting_seq):
    conn = BaseHook.get_connection('api_base_url')
    base_url = f"{conn.schema}://{conn.host}:{conn.port}" if conn.port else f"{conn.schema}://{conn.host}"
    url = f"{base_url}/api/o11y/insight/anomaly-detection/{setting_seq}"

    LOGGER.info(f"[post_to_api] SEQ={setting_seq} -> URL={url}")
    if not conn.host or not conn.schema:
        LOGGER.error(f"[post_to_api] Invalid connection (schema={conn.schema}, host={conn.host}, port={conn.port})")
    try:
        response = requests.post(url, timeout=15)
        LOGGER.info(f"[post_to_api] SEQ={setting_seq} status={response.status_code}")
        LOGGER.debug(f"[post_to_api] Response body(head 500): {response.text[:500] if response.text else '<empty>'}")
        if response.status_code != 200:
            LOGGER.warning(f"[post_to_api] Failed SEQ={setting_seq} code={response.status_code}")
    except Exception as e:
        LOGGER.exception(f"[post_to_api] Exception SEQ={setting_seq}: {e}")

def execute_api_calls(**context):
    ti = context['ti']
    seq_list = ti.xcom_pull(task_ids='filter_records')

    if not seq_list:
        LOGGER.info("[execute_api_calls] No records to execute.")
        return

    LOGGER.info(f"[execute_api_calls] Start API calls for seq_list={seq_list}")
    success = 0
    fail = 0

    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = {executor.submit(post_to_api, setting_seq): setting_seq for setting_seq in seq_list}
        for future in as_completed(futures):
            setting_seq = futures[future]
            try:
                future.result()
                success += 1
            except Exception as exc:
                fail += 1
                LOGGER.exception(f"[execute_api_calls] Exception for settingSeq {setting_seq}: {exc}")

    LOGGER.info(f"[execute_api_calls] Finished. success={success}, fail={fail}")

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
