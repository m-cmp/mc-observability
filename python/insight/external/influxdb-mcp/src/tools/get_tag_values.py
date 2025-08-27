from influx_client import InfluxDBClient
import json


def register_tool(mcp, client: InfluxDBClient):
    @mcp.tool()
    def get_tag_values(database_name: str, measurement_name: str, tag_key: str) -> str:
        """
        Retrieves a list of all unique values for a specific tag key within a measurement.

        Tags are indexed metadata used to add context to your time-series data. This tool allows you to
        explore the diversity of that context. For example, if you have a `hostname` tag, this tool will
        return a list of all unique hostnames that have reported data.

        This is extremely useful for discovering the dimensions of your data and for constructing precise
        queries. Before you can filter data with a `WHERE` clause (e.g., `WHERE "hostname" = 'server-a'`),
        you first need to know what valid hostnames exist.

        Use this tool to:
        - Explore the different categories or dimensions within your data.
        - Find specific identifiers (like a host, region, or service name) to use in other queries.
        - Understand the scope and variety of your dataset.

        Args:
            database_name (str): The name of the database where the measurement resides.
            measurement_name (str): The name of the measurement to query.
            tag_key (str): The name of the tag key for which to retrieve all unique values.
        """

        query = f'SHOW TAG VALUES FROM "{measurement_name}" WITH KEY = "{tag_key}"'
        response = client.execute_query(query=query, database=database_name)
        data = json.loads(response)

        try:
            values = [item[1] for item in data["results"][0]["series"][0]["values"]]
            return json.dumps({"tag_values": values})
        except (KeyError, IndexError):
            return json.dumps({"error": f"Could not retrieve tag values for tag '{tag_key}' in measurement '{measurement_name}'."})
