[![MCP Badge](https://lobehub.com/badge/mcp/greenscreen410-influxdb-v1-mcp)](https://lobehub.com/mcp/greenscreen410-influxdb-v1-mcp)

# ⚠️ WARNING: Experimental project

This project is **very unstable** and subject to breaking changes. **Do NOT use in production.**
- Target: InfluxDB v1.x only. For new work, we strongly recommend InfluxDB v3 and official tooling.
- No stability or data-safety guarantees. Use at your own risk.
- APIs, tool names, and behavior may change without notice.
- Limited test coverage; issues are expected.
- If you must try it, run against disposable data and a non-critical environment.
- The code and information in this repository may be inaccurate and may not work as intended.

# InfluxDB-v1-MCP: The MCP Server for InfluxDB v1.x

InfluxDB-v1-MCP is a powerful Model Context Protocol (MCP) interface **specifically designed for InfluxDB v1.x**, enabling AI assistants to intelligently manage and query time-series databases. Going beyond simple data retrieval, this server provides a complete toolkit that allows an AI agent to autonomously explore the database structure, understand data schemas, and execute complex InfluxQL queries.

---

## Table of Contents

- [Overview](#overview)
- [Core Components](#core-components)
- [Available Tools](#available-tools)
- [Embeddings & Vector Store](#embeddings--vector-store)
- [Configuration & Environment Variables](#configuration--environment-variables)
- [Installation & Setup](#installation--setup)
- [Usage Examples](#usage-examples)
- [Integration - Claude desktop/Cursor/Windsurf](#integration---claude-desktopcursorwindsurf)
- [Logging](#logging)
- [Testing](#testing)
---

## Overview

The InfluxDB MCP Server exposes a set of tools for interacting with InfluxDB time-series databases via a standardized protocol. It supports:
- Listing all accessible databases
- Listing all measurements (the equivalent of tables) in a specified database
- Creating new databases
- Retrieving measurement schemas (including fields and tags) to understand data structure
- Executing safe, read-only InfluxQL queries (SELECT, SHOW)

---

## Core Components

- **server.py**: Main MCP server logic and tool definitions.
- **config.py**: Loads configuration from environment and `.env` files.
- **tests/**: Manual and automated test documentation and scripts.

---

## Available Tools

### Standard Database Tools

- **list_databases**
  - Lists all accessible databases.
  - Parameters: _None_
  - ##### Example
    ```json
    {
      "tool_name": "list_databases"
    }
    ```

- **list_measurements**
  - Lists all measurements (the equivalent of tables) in a specified database.
  - Parameters: `database_name` (string, required)
  - ##### Example
    ```json
    {
      "tool_name": "list_measurements",
      "parameters": {
        "database_name": "telegraf"
      }
    }
    ```

- **get_measurement_schema**
  - Retrieves the schema for a measurement (fields, tags, and their types).
  - Parameters: `database_name` (string, required), `measurement_name` (string, required)
  - ##### Example
    ```json
    {
      "tool_name": "get_measurement_schema",
      "parameters": {
        "database_name": "telegraf",
        "measurement_name": "cpu"
      }
    }
    ```

- **execute_influxql**
  - Executes a read-only InfluxQL query (`SELECT`, `SHOW`).
  - Parameters: `influxql_query` (string, required), `database_name` (string, optional)
  - ##### Example
    ```json
    {
      "tool_name": "execute_influxql",
      "parameters": {
        "database_name": "telegraf",
        "influxql_query": "SELECT \"usage_user\" FROM \"cpu\" WHERE time > now() - 1h"
      }
    }
    ```

- **get_last_data_point_timestamp**
  - Retrieves the timestamp of the most recent data point in a given measurement.
  - Parameters: `database_name` (string, required), `measurement_name` (string, required)
  - ##### Example
    ```json
    {
      "tool_name": "get_last_data_point_timestamp",
      "parameters": {
        "database_name": "telegraf",
        "measurement_name": "cpu"
      }
    }
    ```

- **get_tag_values**
  - Retrieves a list of all unique values for a specific tag key within a measurement.
  - Parameters: `database_name` (string, required), `measurement_name` (string, required), `tag_key` (string, required)
  - ##### Example
    ```json
    {
      "tool_name": "get_tag_values",
      "parameters": {
        "database_name": "telegraf",
        "measurement_name": "cpu",
        "tag_key": "cpu"
      }
    }
    ```

- **get_time_window_summary**
  - Calculates summary statistics (mean, max, min, 95th percentile) for a field over a specified time window.
  - Parameters: `database_name` (string, required), `measurement_name` (string, required), `field_key` (string, required), `time_window` (string, required), `filters` (string, optional), `group_by_tags` (string, optional)
  - ##### Example
    ```json
    {
      "tool_name": "get_time_window_summary",
      "parameters": {
        "database_name": "telegraf",
        "measurement_name": "cpu",
        "field_key": "usage_user",
        "time_window": "1h",
        "group_by_tags": "cpu"
      }
    }
    ```
  
---

## Configuration & Environment Variables

All configuration is via environment variables (typically set in a `.env` file):

| Variable               | Description                                            | Required | Default               |
|------------------------|--------------------------------------------------------|----------|-----------------------|
| `INFLUXDB_URL`         | InfluxDB base URL (v1.x)                               | Yes      | `http://localhost:8086` |
| `INFLUXDB_USER`        | InfluxDB username                                       | Yes      |                       |
| `INFLUXDB_PASSWORD`    | InfluxDB password                                       | Yes      |                       |

#### Example `.env` file

```dotenv
INFLUXDB_URL=http://localhost:8086
INFLUXDB_USER=your_db_user
INFLUXDB_PASSWORD=your_db_password
```

---

## Installation & Setup

### Requirements

- **Python 3.11** (see `.python-version`)
- **uv** (dependency manager; [install instructions](https://github.com/astral-sh/uv))
- MariaDB server (local or remote)

### Steps

1. **Clone the repository**
2. **Install `uv`** (if not already):
   ```bash
   pip install uv
   ```
3. **Install dependencies**
   ```bash
   uv pip compile pyproject.toml -o uv.lock
   ```
   ```bash
   uv pip sync uv.lock
   ```
4. **Create `.env`** in the project root (see [Configuration](#configuration--environment-variables))
5. **Run the server**
   ```bash
   python server.py
   ```
   _Adjust entry point if needed (e.g., `main.py`)_

---

## Integration - Claude desktop/Cursor/Windsurf/VSCode

```json
{
  "mcpServers": {
    "influxdb-v1": {
      "command": "uv",
      "args": [
        "--directory",
        "path/to/server/directory/",
        "run",
        "server.py"
        ],
        "envFile": "path/to/mcp-server-mariadb-vector/.env"      
    }
  }
}
```
or
**If already running MCP server**
```json
{
  "servers": {
    "influxdb-v1": {
      "url": "http://{host}:9003/sse",
      "type": "sse"
    }
  }
}
```
---

## Logging

- Logs are written to `logs/mcp_server.log` by default.
- Log messages include tool calls, configuration issues, embedding errors, and client requests.
- Log level and output can be adjusted in the code (see `config.py` and logger setup).
