import json

from config import logger
from influx_client import InfluxDBClient
from utils.timezone_utils import convert_influxdb_result_timezone


def register_tool(mcp, client: InfluxDBClient):
    @mcp.tool()
    def execute_influxql(influxql_query: str, database_name: str = "mc-observability", timezone: str | None = None) -> str:
        """
        Executes a read-only InfluxQL query to fetch raw data.

        This is a powerful and flexible tool for querying time-series data using InfluxQL, an SQL-like
        query language. It allows for complex data retrieval, including filtering with `WHERE` clauses,
        aggregating data with `GROUP BY`, and selecting specific fields and tags.

        **Security Note:** For safety, this tool is restricted to read-only operations.
        Only queries that begin with `SELECT` or `SHOW` are permitted. Any other command
        (e.g., `INSERT`, `DELETE`, `CREATE`) will be blocked.

        Use this tool when you need to:
        - Fetch specific time-series data points.
        - Perform calculations and aggregations on your data.
        - Answer detailed questions that require custom queries beyond the scope of other tools.

        Before using this, you should know the `database_name`, `measurement`, and schema (fields and tags),
        which can be discovered using `list_influxdb_databases`, `list_measurements`, and `get_measurement_schema`.

        Args:
            influxql_query (str): The InfluxQL query to execute. Must start with "SELECT" or "SHOW".
            database_name (str, optional): The name of the database to query. Defaults to "mc-observability".
        """

        logger.info(f"TOOL START: execute_influxql on database '{database_name}'")
        safe_query = influxql_query.strip().upper()
        if not (safe_query.startswith("SELECT") or safe_query.startswith("SHOW")):
            logger.warning(f"Blocked non-read-only query: {influxql_query[:100]}")
            return json.dumps({"error": "SecurityError: Only SELECT and SHOW queries are allowed."})

        raw_text = client.execute_query(influxql_query, database=database_name)
        try:
            parsed = json.loads(raw_text)
            if timezone:
                parsed = convert_influxdb_result_timezone(parsed, timezone)
            result_text = json.dumps(parsed)
        except Exception:
            result_text = raw_text

        logger.info("TOOL END: execute_influxql completed.")
        return result_text
