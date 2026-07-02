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
# Newer mcp-grafana enforces Host-header allowlisting (DNS-rebind protection).
# In-cluster clients reach this server via the compose service name, so that
# host must be allowlisted or every SSE handshake is rejected with 403.
ALLOWED_HOSTS=${MCP_ALLOWED_HOSTS:-"mc-observability-mcp-grafana:8000,localhost:8000,127.0.0.1:8000"}

exec /app/mcp-grafana \
  --transport "${TRANSPORT}" \
  --address "${ADDRESS}" \
  --enabled-tools "${ENABLED_TOOLS}" \
  --allowed-hosts "${ALLOWED_HOSTS}"
