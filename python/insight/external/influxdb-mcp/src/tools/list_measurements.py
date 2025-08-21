from config import logger
from influx_client import InfluxDBClient


def register_tool(mcp, client: InfluxDBClient):
    @mcp.tool()
    def list_measurements(database_name: str) -> str:
        """
        Returns a list of all measurements within a specified database.

        In InfluxDB, a "measurement" is a fundamental part of the data structure, acting as a container for
        time-stamped data points, fields, and tags. It is conceptually similar to a table in a relational database.
        To query data from a measurement, you must first know its name.

        This tool is essential for exploring the structure of a database. After identifying a database with
        `list_databases`, use this tool to see what kinds of time-series data it contains. The output of this
        tool is a prerequisite for more specific queries, such as retrieving data points or examining the schema
        of a particular measurement.

        Args:
            database_name (str): The name of the database to query for measurements. This name can be obtained by using the `list_databases` tool.
        """

        logger.info(f"TOOL START: list_measurements for database '{database_name}'")
        result = client.execute_query("SHOW MEASUREMENTS", database=database_name)
        logger.info(f"TOOL END: list_measurements for database '{database_name}' completed.")
        return result
