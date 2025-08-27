from influx_client import InfluxDBClient
import json
from utils.timezone_utils import convert_rfc3339_to_timezone


def register_tool(mcp, client: InfluxDBClient):
    @mcp.tool()
    def get_last_data_point_timestamp(database_name: str, measurement_name: str, timezone: str | None = None) -> str:
        """
        Retrieves the timestamp of the most recent data point in a given measurement.

        This tool is crucial for monitoring data freshness and ensuring that data sources are actively reporting.
        By fetching the timestamp of the very last data point, you can quickly determine if a measurement is
        receiving new data as expected or if there might be a lag or an outage.

        The tool works by first identifying an arbitrary field within the measurement and then using the
        `LAST()` function in InfluxQL to efficiently find the timestamp of the last recorded value for that field.
        This assumes that all fields in a measurement are updated together.

        Use this tool to:
        - Verify that a data pipeline is working correctly.
        - Check the health and recency of your time-series data.
        - Trigger alerts if data becomes stale.

        Args:
            database_name (str): The name of the database containing the measurement.
            measurement_name (str): The name of the measurement to check for the last data point.
        """

        # 1. 측정값에서 필드 키를 가져옵니다.
        field_keys_query = f'SHOW FIELD KEYS FROM "{measurement_name}"'
        field_keys_response = client.execute_query(query=field_keys_query, database=database_name)
        field_keys_data = json.loads(field_keys_response)

        try:
            # 응답에서 첫 번째 필드 키를 추출합니다.
            field_key = field_keys_data["results"][0]["series"][0]["values"][0][0]
        except (KeyError, IndexError):
            return json.dumps({"error": f"No fields found for measurement '{measurement_name}'."})

        # 2. 마지막 데이터 포인트 쿼리를 실행합니다.
        last_point_query = f'SELECT last("{field_key}") FROM "{measurement_name}"'
        last_point_response = client.execute_query(query=last_point_query, database=database_name)
        last_point_data = json.loads(last_point_response)

        try:
            # 응답에서 타임스탬프를 추출합니다.
            timestamp = last_point_data["results"][0]["series"][0]["values"][0][0]
            if timezone:
                timestamp = convert_rfc3339_to_timezone(timestamp, timezone)
            return json.dumps({"last_data_point_timestamp": timestamp})
        except (KeyError, IndexError):
            return json.dumps({"error": f"No data points found for measurement '{measurement_name}'."})
