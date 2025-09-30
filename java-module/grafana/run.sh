#!/bin/bash

echo "[*] Running Grafana Server..."
/run.sh &
cnt=0
while true
do
    (( cnt = "$cnt" + 1 ))

    if ps aux | grep -i grafana | grep -v grep > /dev/null; then
        echo "[*] Grafana process is running"
        break
    fi

    if [ "$cnt" = "30" ]; then
        echo "[!] Grafana process not found after 30 seconds."
        exit 1
    fi
    sleep 1
done

echo "[*] Logging in to Grafana..."
cnt=0
while true
do
    (( cnt = "$cnt" + 1 ))
    RESPONSE=$(curl -s -w "\n%{http_code}" -c ~/grafana-cookie -XPOST \
        -H 'Accept: */*' \
        -H 'Content-Type: application/json' \
        "http://127.0.0.1:3000/login" \
        --data-raw "{\"user\":\"$GF_SECURITY_ADMIN_USER\",\"password\":\"$GF_SECURITY_ADMIN_PASSWORD\"}")

    HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
    if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
        break
    fi
    if [ "$cnt" = "120" ]; then
        echo "[!] Failed to login to Grafana."
        exit 1
    fi
    sleep 1
done
echo "[*] Successfully logged in to Grafana!"

echo "[*] Creating service account..."
RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/api/serviceaccounts/search?perpage=10&page=1&query=o11y" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json')

GRAFANA_PREVIOUS_UID=$(echo $RESPONSE | sed -n 's/.*"uid":"\([^"]*\)".*/\1/p')
if [ "$GRAFANA_PREVIOUS_UID" != "" ]; then
  HTTP_STATUS=$(curl -s -w "\n%{http_code}" -b ~/grafana-cookie -XDELETE \
      "http://127.0.0.1:3000/api/serviceaccounts/$GRAFANA_PREVIOUS_UID" \
      -H 'Accept: */*' \
      -H 'Content-Type: application/json' \
      -H 'Origin: http://127.0.0.1:3000' \
      -o /dev/null | tail -n1)
  if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
    echo "[*] Successfully deleted previous service account!"
  else
    echo "[!] Failed to delete previous service account."
    exit 1
  fi
fi

HTTP_STATUS=$(curl -s -w "\n%{http_code}" -b ~/grafana-cookie -XPOST \
    "http://127.0.0.1:3000/api/serviceaccounts" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000' \
    -d '{"name":"o11y","role":"Admin"}' \
    -o /dev/null | tail -n1)
if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
  echo "[*] Successfully created the service account!"
else
  echo "[!] Failed to create the service account."
  exit 1
fi

RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/api/serviceaccounts/search?perpage=10&page=1&query=o11y" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -d '{"name":"o11y","role":"Admin"}')
GRAFANA_UID=$(echo $RESPONSE | sed -n 's/.*"uid":"\([^"]*\)".*/\1/p')

echo "[*] Creating token..."
RESPONSE=$(curl -s -b ~/grafana-cookie -XPOST \
    "http://127.0.0.1:3000/api/serviceaccounts/$GRAFANA_UID/tokens" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000' \
    -d '{"name":"o11y-token"}')
export GRAFANA_TOKEN=$(echo $RESPONSE | sed -n 's/.*"key":"\([^"]*\)".*/\1/p')
echo GRAFANA_TOKEN=$GRAFANA_TOKEN > ~/env.grafana

echo "[*] Getting folders..."
RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/api/folders" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000')
FOLDER_UID=$(echo $RESPONSE | tr ',' '\n' | grep -B2 '"title":"o11yFolder"' | grep '"uid":"' | sed 's/.*"uid":"\([^"]*\)".*/\1/')

if [ "$FOLDER_UID" != "" ]; then
  echo "[*] Folder already exist!"
else
  echo "[*] Creating folder..."
  HTTP_STATUS=$(curl -s -w "\n%{http_code}" -b ~/grafana-cookie -XPOST \
      "http://127.0.0.1:3000/api/folders" \
      -H 'Accept: */*' \
      -H 'Content-Type: application/json' \
      -H 'Origin: http://127.0.0.1:3000' \
      -d '{"title":"o11yFolder"}' \
      -o /dev/null | tail -n1)
  if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
    echo "[*] Successfully created the folder!"
  else
    echo "[!] Failed to create the folder."
    exit 1
  fi
fi

RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/api/folders" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000')
FOLDER_UID=$(echo $RESPONSE | tr ',' '\n' | grep -B2 '"title":"o11yFolder"' | grep '"uid":"' | sed 's/.*"uid":"\([^"]*\)".*/\1/')
echo FOLDER_UID=$FOLDER_UID >> ~/env.grafana

echo "[*] Getting receivers..."
RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/apis/notifications.alerting.grafana.app/v0alpha1/namespaces/default/receivers" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000')
GRAFANA_O11Y_RECEIVER_UID=$(echo $RESPONSE | sed -n 's/.*"title": "\([^"]*o11y\)".*/\1/p')

if [ "$GRAFANA_O11Y_RECEIVER_UID" != "" ]; then
    echo "[*] o11y receiver already exist!"
else
  echo "[*] Creating receiver..."
  HTTP_STATUS=$(curl -s -w "\n%{http_code}" -b ~/grafana-cookie -XPOST \
      "http://127.0.0.1:3000/apis/notifications.alerting.grafana.app/v0alpha1/namespaces/default/receivers" \
      -H 'Accept: */*' \
      -H 'Content-Type: application/json' \
      -H 'Origin: http://127.0.0.1:3000' \
      -d '{"metadata":{},"spec":{"title":"o11y","integrations":[{"type":"webhook","name":"o11y","disableResolveMessage":false,"secureFields":{"authorization_credentials":true,"tlsConfig.caCertificate":true,"tlsConfig.clientCertificate":true,"tlsConfig.clientKey":true},"settings":{"url":"'"$ALARM_END_POINT_URL"'","httpMethod":"POST","username":"'"$ALARM_END_POINT_USERNAME"'","tlsConfig":{"insecureSkipVerify":true},"password":"'"$ALARM_END_POINT_PASSWORD"'"}}]}}' | tail -n1)
  if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
    echo "[*] Successfully created the receiver!"
  else
    echo "[!] Failed to create the receiver."
    exit 1
  fi
fi

RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/apis/notifications.alerting.grafana.app/v0alpha1/namespaces/default/receivers" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000')
GRAFANA_O11Y_RECEIVER_UID=$(echo $RESPONSE | sed -n 's/.*"title": "\([^"]*o11y\)".*/\1/p')
echo GRAFANA_O11Y_RECEIVER_UID=$GRAFANA_O11Y_RECEIVER_UID >> ~/env.grafana

RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/api/datasources/name/Loki" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000')
DATA_SOURCE_LOKI_UID=$(echo $RESPONSE | sed -n 's/.*"uid":"\([^"]*\)".*/\1/p')
echo DATA_SOURCE_LOKI_UID=$DATA_SOURCE_LOKI_UID >> ~/env.grafana

RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/api/datasources/name/InfluxDB" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000')
DATA_SOURCE_INFLUXDB_UID=$(echo $RESPONSE | sed -n 's/.*"uid":"\([^"]*\)".*/\1/p')
echo DATA_SOURCE_INFLUXDB_UID=$DATA_SOURCE_INFLUXDB_UID >> ~/env.grafana

echo "[*] Checking existing rules..."
RESPONSE=$(curl -s -b ~/grafana-cookie -XGET \
    "http://127.0.0.1:3000/api/ruler/grafana/api/v1/rules?subtype=cortex" \
    -H 'Accept: application/json, text/plain, */*' \
    -H 'Content-Type: application/json' \
    -H 'Origin: http://127.0.0.1:3000')

LOG_RULE_EXISTS=$(echo $RESPONSE | grep -q '"log"' && echo "true" || echo "false")
METRIC_RULE_EXISTS=$(echo $RESPONSE | grep -q '"metric"' && echo "true" || echo "false")

if [ "$LOG_RULE_EXISTS" = "true" ]; then
    echo "[*] Log rule already exists, skipping initialization."
else
    echo "[*] Initializing log rule..."
    HTTP_STATUS=$(curl -s -w "\n%{http_code}" -b ~/grafana-cookie -XPOST \
        "http://127.0.0.1:3000/api/ruler/grafana/api/v1/rules/$FOLDER_UID?subtype=cortex" \
        -H 'Accept: */*' \
        -H 'Content-Type: application/json' \
        -H 'Origin: http://127.0.0.1:3000' \
        -d '{"name":"log","rules":[{"grafana_alert":{"title":"log-rule-init","condition":"C","data":[{"refId":"A","datasourceUid":"'$DATA_SOURCE_LOKI_UID'","queryType":"range","relativeTimeRange":{"from":600,"to":0},"model":{"refId":"A","instant":true,"expr":"{job=\"non-existent-job\", uuid=\"definitely-not-real\"} |= \"never-happens-keyword\"","queryType":"range","editorMode":"code","direction":"backward"}},{"datasourceUid":"__expr__","model":{"refId":"B","datasource":{"type":"__expr__","uid":"__expr__","name":"Expression"},"type":"reduce","reducer":"last","conditions":[{"type":"query","reducer":{"params":[],"type":"avg"},"operator":{"type":"and"},"query":{"params":[]},"evaluator":{"params":[0,0],"type":"gt"}}],"expression":"A"},"refId":"B","queryType":"expression"},{"refId":"C","datasourceUid":"__expr__","queryType":"","model":{"refId":"C","type":"threshold","datasource":{"uid":"__expr__","type":"__expr__"},"conditions":[{"type":"query","evaluator":{"params":[0],"type":"gt"},"operator":{"type":"and"},"query":{"params":["C"]},"reducer":{"params":[],"type":"last"}}],"expression":"B"}}],"is_paused":true,"no_data_state":"NoData","exec_err_state":"Error","notification_settings":{"receiver":"o11y"}},"annotations":{},"labels":{},"for":"0s"}],"interval":"10m"}' | tail -n1)
    if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
        echo "[*] Successfully initialized log rule!"
    else
        echo "[!] Failed to initialize log rule."
        exit 1
    fi
fi

if [ "$METRIC_RULE_EXISTS" = "true" ]; then
    echo "[*] Metric rule already exists, skipping initialization."
else
    echo "[*] Initializing metric rule..."
    HTTP_STATUS=$(curl -s -w "\n%{http_code}" -b ~/grafana-cookie -XPOST \
        "http://127.0.0.1:3000/api/ruler/grafana/api/v1/rules/$FOLDER_UID?subtype=cortex" \
        -H 'Accept: */*' \
        -H 'Content-Type: application/json' \
        -H 'Origin: http://127.0.0.1:3000' \
        -d '{"name":"metric","rules":[{"grafana_alert":{"title":"metric-rule-init","condition":"C","data":[{"refId":"A","relativeTimeRange":{"from":600,"to":0},"queryType":"","datasourceUid":"'$DATA_SOURCE_INFLUXDB_UID'","model":{"refId":"A","hide":false,"datasource":{"uid":"'$DATA_SOURCE_INFLUXDB_UID'","type":"influxdb"},"instant":true,"query":"SELECT 100-mean(\"usage_idle\") FROM \"cpu\" GROUP BY \"host\"","rawQuery":true,"resultFormat":"time_series"}},{"refId":"C","datasourceUid":"__expr__","queryType":"","model":{"refId":"C","type":"threshold","datasource":{"uid":"__expr__","type":"__expr__"},"conditions":[{"type":"query","evaluator":{"params":[0],"type":"gt"},"operator":{"type":"and"},"query":{"params":["C"]},"reducer":{"params":[],"type":"last"}}],"expression":"A"}}],"is_paused":true,"no_data_state":"NoData","exec_err_state":"Error","notification_settings":{"receiver":"o11y"}},"annotations":{},"labels":{},"for":"0s"}],"interval":"1m"}' | tail -n1)
    if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
        echo "[*] Successfully initialized metric rule!"
    else
        echo "[!] Failed to initialize metric rule."
        exit 1
    fi
fi

echo "[*] Deleting cookie..."
rm -f ~/grafana-cookie

if [ -d /grafana_config ]; then
    if [ -f ~/env.grafana ]; then
        echo "[*] Copying ~/env.grafana to /grafana_config/env.grafana..."
        cp ~/env.grafana /grafana_config/env.grafana

        if [ $? -eq 0 ]; then
            echo "[*] Successfully copied env.grafana to shared volume"
            chmod 644 /grafana_config/env.grafana
        else
            echo "[!] Failed to copy env.grafana"
            exit 1
        fi
    else
        echo "[!] Source file ~/env.grafana does not exist"
        exit 1
    fi
else
    echo "[!] Target directory /grafana_config does not exist"
    exit 1
fi

echo "[*] Running NGINX SSL proxy..."
nginx -g 'daemon off;' > /dev/null
