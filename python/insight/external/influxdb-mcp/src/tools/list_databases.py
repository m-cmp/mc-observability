from config import logger
from influx_client import InfluxDBClient


def register_tool(mcp, client: InfluxDBClient):
    @mcp.tool()
    def list_databases() -> str:
        """
        Lists all available InfluxDB databases that the current user has permission to access.

        In InfluxDB, a database is a logical container for time-series data, users, and retention policies.
        It's the top-level organizational unit. Before you can query data or write new data, you
        must know which database to target.

        This tool is a critical first step in interacting with an InfluxDB instance, as it provides
        the necessary database names for use in other tools that require a `database_name` parameter.
        For example, to list measurements within a database, you first need to get the database name using this tool.

        The output is a string representation of a list of database names.
        """

        logger.info("TOOL START: list_databases")
        result = client.execute_query("SHOW DATABASES")
        logger.info("TOOL END: list_databases completed.")
        return result
