#!/bin/sh

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

CONFIG_DIR="/app-config"
DOCKER_CONFIG="$CONFIG_DIR/application-docker.yaml"

if [ -z "$O11Y_EXTERNAL_IP" ]; then
    echo "O11Y_EXTERNAL_IP not set. Fetching external IP..."

    O11Y_EXTERNAL_IP=$(curl -s ifconfig.me)

    if [ -z "$O11Y_EXTERNAL_IP" ]; then
        O11Y_EXTERNAL_IP=$(curl -s api.ipify.org)
    fi

    if [ -z "$O11Y_EXTERNAL_IP" ]; then
        O11Y_EXTERNAL_IP=$(curl -s icanhazip.com)
    fi

    echo "Fetched external IP: $O11Y_EXTERNAL_IP"
else
    echo "Using provided O11Y_EXTERNAL_IP: $O11Y_EXTERNAL_IP"
fi

if [ ! -z "$O11Y_EXTERNAL_IP" ]; then
  export LOKI_URL="http://$O11Y_EXTERNAL_IP:3100"
  export INFLUX1_URL="http://$O11Y_EXTERNAL_IP:8086"
  export INFLUX2_URL="http://$O11Y_EXTERNAL_IP:8087"
fi

echo "Starting application..."
exec java -jar -Dspring.config.location=file:/app-config/ /mc-o11y-manager.jar
