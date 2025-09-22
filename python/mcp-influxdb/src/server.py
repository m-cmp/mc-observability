# main.py
import argparse
import importlib
import logging
import os

from influx_client import InfluxDBClient
from mcp.server.fastmcp import FastMCP

logger = logging.getLogger(__name__)

# --- Server and client initialization ---
mcp = FastMCP(
    title="InfluxDBv1-MCP (Auto-Discovery & Robust Path)",
    description="MCP server based on auto-discovery that works reliably regardless of execution path.",
    version="6.1.0",
    host="0.0.0.0",
    port=8002,
)

client = InfluxDBClient()


# --- 1. Create absolute path to 'tools' directory based on current script location ---
# __file__ refers to the path of the current script file (main.py)
script_dir = os.path.dirname(os.path.abspath(__file__))
tools_dir = os.path.join(script_dir, "tools")

# --- 2. Logic to automatically find and register all tools from 'tools' directory ---
logger.info("--- Starting automatic tool discovery ---")
logger.info(f"Target directory for discovery: {tools_dir}")

try:
    for filename in os.listdir(tools_dir):
        # Target only Python files and exclude special files like __init__.py
        if filename.endswith(".py") and not filename.startswith("__"):
            # Now module path needs to be loaded based on absolute filesystem path
            # rather than relative path like 'tools.list_influxdb_databases',
            # so we use module loader directly for more stable approach (logic below is more robust)
            module_name = f"tools.{filename[:-3]}"

            try:
                module = importlib.import_module(module_name)

                if hasattr(module, "register_tool"):
                    register_function = getattr(module, "register_tool")
                    register_function(mcp, client)
                    logger.info(f"Successfully registered '{module_name}' tool.")
                else:
                    logger.warning(f"Skipping '{module_name}' module - no 'register_tool' function found.")

            except Exception as e:
                logger.error(f"Error occurred while registering '{module_name}' tool: {e}")

except FileNotFoundError:
    logger.error(f"Fatal error: Cannot find '{tools_dir}' directory. Please check file structure.")
except Exception as e:
    logger.error(f"Unexpected error occurred during tool loading: {e}")


logger.info("--- All tools registration completed ---")

# --- Server execution ---
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="InfluxDB v1 MCP Server")
    parser.add_argument("--transport", choices=["stdio", "sse"], default="stdio")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    if args.transport == "sse":
        # Use FastMCP SSE mode (host is set internally)
        mcp.run(transport="sse")
    else:
        mcp.run(transport="stdio")
