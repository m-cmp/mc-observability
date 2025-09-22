import json

from influx_client import InfluxDBClient
from utils.timezone_utils import convert_influxdb_result_timezone


def register_tool(mcp, client: InfluxDBClient):
    @mcp.tool()
    def get_time_window_summary(database_name: str, measurement_name: str, field_key: str, time_window: str, filters: str = None, group_by_tags: str = None, timezone: str | None = None) -> str:
        """
        Calculates summary statistics (mean, max, min, 95th percentile) for a field over a specified time window.

        This tool provides a powerful way to get a high-level overview of a metric's behavior without fetching
        the raw data. It is highly efficient for trend analysis, performance monitoring, and anomaly detection.
        The function constructs an InfluxQL query to compute the mean, maximum, minimum, and 95th percentile
        for the specified field.

        Use this tool to answer questions like:
        - "What was the average CPU usage over the last hour?"
        - "What was the peak memory consumption yesterday, broken down by server?"
        - "Show me the 95th percentile latency for the web-api service in the last 30 minutes."

        Args:
            database_name (str): The name of the database to query.
            measurement_name (str): The name of the measurement containing the data.
            field_key (str): The numerical field for which to calculate the statistics.
            time_window (str): The duration to look back from now (e.g., '1h', '7d', '30m').
            filters (str, optional): Additional `WHERE` clause conditions to apply, specified as a string (e.g., `"hostname" = 'server1' AND "region" = 'us-west'`). Defaults to None.
            group_by_tags (str, optional): A comma-separated list of tag keys to group the results by, enabling dimensional analysis (e.g., 'hostname,region'). Defaults to None.
        """

        # Default aggregation functions
        aggregations = f'mean("{field_key}") AS "mean", max("{field_key}") AS "max", min("{field_key}") AS "min", percentile("{field_key}", 95) AS "p95"'

        # Build WHERE clause
        where_clause = f"WHERE time > now() - {time_window}"
        if filters:
            where_clause += f" AND {filters}"

        # Build GROUP BY clause
        group_by_clause = ""
        if group_by_tags:
            group_by_clause = f"GROUP BY {group_by_tags}"

        # Combine complete query
        query = f'SELECT {aggregations} FROM "{measurement_name}" {where_clause} {group_by_clause}'

        raw_text = client.execute_query(query=query, database=database_name)
        try:
            parsed = json.loads(raw_text)
            if timezone:
                parsed = convert_influxdb_result_timezone(parsed, timezone)
            return json.dumps(parsed)
        except Exception:
            return raw_text
