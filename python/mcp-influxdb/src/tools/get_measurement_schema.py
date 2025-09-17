import json

from config import logger
from influx_client import InfluxDBClient


def register_tool(mcp, client: InfluxDBClient):
    @mcp.tool()
    def get_measurement_schema(measurement_name: str, database_name: str) -> str:
        """
        Returns the schema of a specific measurement, detailing its fields and tags.

        In InfluxDB, the schema defines the structure of the data within a measurement. It consists of:
        - **Fields**: The actual data values being recorded (e.g., temperature, CPU usage). Field values can be floats, integers, strings, or booleans.
        - **Tags**: Indexed metadata that provides context for the data (e.g., host, region, sensor_id). Tags are always strings and are used for efficient filtering and grouping.

        Understanding the schema is absolutely essential before attempting to query data. Without knowing the available fields and tags, you cannot construct a meaningful `SELECT` statement or apply `WHERE` clauses. This tool is a prerequisite for any data retrieval task.

        Use this tool to:
        - Discover what data is available within a measurement.
        - Understand the data types of your fields.
        - Identify the available tags for filtering and grouping.

        Args:
            measurement_name (str): The name of the measurement for which to retrieve the schema.
            database_name (str): The name of the database where the measurement resides.
        """

        logger.info(f"TOOL START: get_measurement_schema for '{database_name}'.'{measurement_name}'")
        fields_query = f'SHOW FIELD KEYS FROM "{measurement_name}"'
        tags_query = f'SHOW TAG KEYS FROM "{measurement_name}"'

        fields_result_text = client.execute_query(fields_query, database=database_name)
        tags_result_text = client.execute_query(tags_query, database=database_name)

        try:
            fields_data = json.loads(fields_result_text)
            tags_data = json.loads(tags_result_text)
            schema = {
                "fields": fields_data.get("results", [{}])[0].get("series", [{}])[0].get("values", []),
                "tags": tags_data.get("results", [{}])[0].get("series", [{}])[0].get("values", []),
            }
            logger.info(f"TOOL END: get_measurement_schema completed successfully for '{database_name}'.'{measurement_name}'.")
            return json.dumps(schema)
        except (json.JSONDecodeError, IndexError, KeyError) as e:
            logger.error(f"Error parsing schema for '{database_name}'.'{measurement_name}': {e}")
            return json.dumps({"error": "Failed to parse schema", "raw_fields": fields_result_text, "raw_tags": tags_result_text})
