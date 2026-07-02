#!/bin/bash

echo "Waiting for Grafana environment file..."

ENV_FILE="/grafana_config/env.grafana"
TIMEOUT=300
ELAPSED=0

while [ ! -f "$ENV_FILE" ] && [ $ELAPSED -lt $TIMEOUT ]; do
    echo "Waiting for $ENV_FILE to be created... ($ELAPSED seconds)"
    sleep 5
    ELAPSED=$((ELAPSED + 5))
done

if [ -f "$ENV_FILE" ]; then
    echo "Found Grafana environment file. Loading environment variables..."

    export $(grep -v '^#' $ENV_FILE | xargs)

    echo "Loaded Grafana environment variables."
else
    echo "WARNING: Grafana environment file not found within timeout period!"
    echo "Proceeding without Grafana environment..."
fi

ADDRESS=${MCP_ADDRESS:-"0.0.0.0:8000"}
TRANSPORT=${MCP_TRANSPORT:-"sse"}
ENABLED_TOOLS=${MCP_ENABLED_TOOLS:-"loki,datasource"}

exec /app/mcp-grafana \
  --transport "${TRANSPORT}" \
  --address "${ADDRESS}" \
  --enabled-tools "${ENABLED_TOOLS}"
