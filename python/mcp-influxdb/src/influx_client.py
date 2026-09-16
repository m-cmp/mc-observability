# influx_client.py
import json
import os
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urlparse, urlunparse

import requests
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
            INFLUXDB_URLS: Comma-separated InfluxDB server URLs queried together when set.
            INFLUXDB_URL: The URL of the InfluxDB server.
            INFLUXDB_USER: The username for authentication.
            INFLUXDB_PASSWORD: The password for authentication.
            INFLUXDB_DATABASE: The default database name.
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
        # Infra and node metrics can reside on different servers, so query every
        # configured endpoint. The single-URL setting above remains the fallback.
        listed = [url.strip().rstrip("/") for url in (os.getenv("INFLUXDB_URLS") or "").split(",") if url.strip()]
        self.endpoints = listed or [self.base_url]
        self.user = os.getenv("INFLUXDB_USER") or "mc-agent"
        self.password = os.getenv("INFLUXDB_PASSWORD") or "mc-agent"
        self.database = os.getenv("INFLUXDB_DATABASE") or "mc-observability"

    def execute_query(self, query: str, database: str | None = None) -> str:
        """Query every configured server; keep SELECT series separate by server."""
        params = {"u": self.user, "p": self.password, "q": query, "db": database or self.database}

        def fetch(endpoint):
            index, url = endpoint
            server = urlparse(url).netloc.rsplit("@", 1)[-1] or f"influxdb-{index}"
            try:
                response = requests.get(
                    f"{url}/query", params=params, headers={"Accept": "application/json"}, timeout=20
                )
                response.raise_for_status()
                payload = response.json()
                error = payload.get("error") or next(
                    (result.get("error") for result in payload.get("results") or [] if result.get("error")), None
                )
                if error:
                    return {"server": server, "status": "error", "error": str(error)[:200]}, None
                has_series = any(result.get("series") for result in payload.get("results") or [])
                return {"server": server, "status": "success" if has_series else "no_data"}, payload
            except requests.exceptions.HTTPError as exc:
                return {"server": server, "status": "error", "error": f"HTTP {exc.response.status_code}"}, None
            except Exception as exc:
                return {"server": server, "status": "error", "error": type(exc).__name__}, None

        with ThreadPoolExecutor(max_workers=len(self.endpoints)) as executor:
            answers = list(executor.map(fetch, enumerate(self.endpoints, start=1)))
        servers = [status for status, _ in answers]
        failures = [status for status in servers if status["status"] == "error"]
        payloads = [(status["server"], payload) for status, payload in answers if payload is not None]
        # With a server missing, an empty answer is not proof of an empty window.
        if failures and not any(status["status"] == "success" for status in servers):
            return json.dumps({"status": "error", "error": "; ".join(
                f'{status["server"]}: {status["error"]}' for status in failures
            ), "servers": servers})
        merged = merge_results(payloads, deduplicate=query.lstrip().upper().startswith("SHOW"))
        return json.dumps({"status": "partial" if failures else "success", "data": merged, "servers": servers})


def merge_results(payloads: list[tuple[str, dict]], *, deduplicate: bool) -> dict:
    """Union SHOW rows, but retain each server's independent SELECT result."""
    series: dict = {}
    separate: list = []
    for server, payload in payloads:
        for result in payload.get("results") or []:
            for item in result.get("series") or []:
                if not deduplicate:
                    separate.append({**item, "server": server})
                    continue
                key = (
                    item.get("name"),
                    tuple(item.get("columns") or []),
                    tuple(sorted((item.get("tags") or {}).items())),
                )
                rows = series.setdefault(key, {**item, "values": {}})["values"]
                for row in item.get("values") or []:
                    rows.setdefault(tuple(row), row)
    merged = {"statement_id": 0}
    if separate:
        merged["series"] = separate
    elif series:
        merged["series"] = [{**item, "values": list(item["values"].values())} for item in series.values()]
    return {"results": [merged]}
