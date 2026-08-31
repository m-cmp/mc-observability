import json
import re

from config import logger
from influx_client import InfluxDBClient
from utils.timezone_utils import convert_influxdb_result_timezone


def register_tool(mcp, client: InfluxDBClient):
    @mcp.tool()
    def execute_influxql(
        influxql_query: str,
        timezone: str | None = None,
        database_name: str | None = None,
    ) -> str:
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
            timezone (str, optional): Timezone for timestamp conversion.
            database_name (str, optional): Database to query. Uses the configured default when omitted.
        """

        logger.info("TOOL START: execute_influxql")
        query = influxql_query.strip()
        statement = query[:-1].rstrip() if query.endswith(";") else query
        first_token = re.match(r"[A-Za-z_][A-Za-z0-9_]*", statement)
        if (
            not first_token
            or first_token.group().upper() not in {"SELECT", "SHOW"}
            or ";" in statement
            or re.search(r"\bINTO\b", statement, re.IGNORECASE)
        ):
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
