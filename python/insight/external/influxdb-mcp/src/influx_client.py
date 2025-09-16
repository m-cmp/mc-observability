# influx_client.py
import requests
import json
import os
from urllib.parse import urlparse, urlunparse
from dotenv import load_dotenv

# Ensure environment variables from .env are loaded even if config isn't imported yet
load_dotenv()


class InfluxDBClient:
    """
    A client class for managing all HTTP communications with InfluxDB v1.x.
    This class encapsulates connection details and provides clear methods for interacting with the database.
    It uses environment variables for configuration, with fallback defaults.

    Attributes:
        base_url (str): The base URL of the InfluxDB instance (default: "http://localhost:8086").
        user (str): The username for authentication (default: "mc-agent").
        password (str): The password for authentication (default: "mc-agent").
    """

    def __init__(self):
        """
        Initializes the InfluxDBClient with connection details from environment variables.
        If environment variables are not set, fallback defaults are used.

        Environment Variables:
            INFLUXDB_URL: The URL of the InfluxDB server.
            INFLUXDB_USER: The username for authentication.
            INFLUXDB_PASSWORD: The password for authentication.
        """
        # Load settings with robust fallbacks and normalization
        raw_url = os.getenv("INFLUXDB_URL") or ""
        host = os.getenv("INFLUXDB_HOST") or ""
        port = os.getenv("INFLUXDB_PORT") or ""

        if not raw_url:
            if host:
                # If only host/port provided, construct URL with defaults
                default_port = str(port or 8086)
                if host.startswith("http://") or host.startswith("https://"):
                    raw_url = f"{host}:{default_port}"
                else:
                    raw_url = f"http://{host}:{default_port}"
            else:
                raw_url = "http://localhost:8086"

        if not raw_url.startswith("http://") and not raw_url.startswith("https://"):
            raw_url = f"http://{raw_url}"

        parsed = urlparse(raw_url)
        # If port is missing, default to 8086 for InfluxDB v1
        if parsed.port is None and parsed.scheme in ("http", "https") and parsed.hostname:
            netloc = parsed.hostname
            if parsed.username and parsed.password:
                netloc = f"{parsed.username}:{parsed.password}@{netloc}"
            netloc = f"{netloc}:8086"
            parsed = parsed._replace(netloc=netloc)
            raw_url = urlunparse(parsed)

        # Remove trailing slash to avoid double slashes in requests
        self.base_url = raw_url.rstrip("/")

        self.user = os.getenv("INFLUXDB_USER") or "mc-agent"
        self.password = os.getenv("INFLUXDB_PASSWORD") or "mc-agent"

    def execute_query(self, query: str, database: str = None) -> str:
        """
        Executes a query on InfluxDB and returns the result as a JSON formatted string.

        This method sends a GET request to the InfluxDB query endpoint with the provided query.
        Authentication is handled using the configured username and password.

        Args:
            query (str): The InfluxQL query to execute.
            database (str, optional): The name of the database to query. If not provided, the query
                                      must specify the database internally or use the default.

        Returns:
            str: A JSON-formatted string containing the query result or an error message.

        Raises:
            requests.exceptions.HTTPError: If the HTTP request fails (handled internally and returned as JSON).
            Exception: For other unexpected errors (handled internally and returned as JSON).
        """
        try:
            params = {
                "u": self.user,
                "p": self.password,
                "q": query,
            }
            if database:
                params["db"] = database

            response = requests.get(f"{self.base_url}/query", params=params, headers={"Accept": "application/json"})
            response.raise_for_status()

            # Parse response from InfluxDB as JSON
            # Create JSON structure representing successful response
            return json.dumps({"status": "success", "data": response.json()})

        except requests.exceptions.HTTPError as http_err:
            return json.dumps(
                {
                    "status": "error",
                    "error": "HTTP Error",
                    "message": f"Status Code: {http_err.response.status_code}",
                    "response_body": http_err.response.text,
                }
            )
        except Exception as e:
            return json.dumps({"status": "error", "error": "Exception", "message": str(e)})
