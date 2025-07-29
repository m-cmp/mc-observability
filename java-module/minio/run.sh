#!/bin/bash

export RUN_PATH="/minio"

/minio/bin/minio server /minio/data --console-address :9001 > /dev/null &

LOKI_USER=lokiuser
LOKI_USER_PASSWORD=lokipass
LOKI_USER_ACCESS_KEY_ID=YmQYMMfCAPenMclY1WR1
LOKI_USER_SECRET_ACCESS_KEY=iXjthlJdpI0fKpz015yMgBKuNPBtSrIvDOED1a3P

echo "[*] Logging in to MinIO..."
cnt=0
while true
do
    (( cnt = "$cnt" + 1 ))
    RESPONSE=$(curl -s -w "\n%{http_code}" -c $RUN_PATH/minio-cookie -XPOST \
        -H 'Accept: */*' \
        -H 'Content-Type: application/json' \
        "http://127.0.0.1:9001/api/v1/login" \
        --data-raw "{\"accessKey\":\"$MINIO_ROOT_USER\",\"secretKey\":\"$MINIO_ROOT_PASSWORD\"}")

    HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
    if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
        break
    fi
    if [ "$cnt" = "30" ]; then
        echo "[!] Failed to login to MinIO."
        exit 1
    fi
    sleep 1
done
echo "[*] Successfully logged in to MinIO!"

echo "[*] Checking if bucket 'loki-data' exists..."
BUCKET_CHECK=$(curl -s -w "%{http_code}" -b $RUN_PATH/minio-cookie -XGET \
    "http://127.0.0.1:9001/api/v1/buckets" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json')

BUCKET_HTTP_STATUS=$(echo "$BUCKET_CHECK" | tail -n1)
BUCKET_RESPONSE=$(echo "$BUCKET_CHECK" | head -n -1)

if [[ $BUCKET_HTTP_STATUS =~ ^2[0-9][0-9]$ ]] && echo "$BUCKET_RESPONSE" | grep -q '"name":"loki-data"'; then
    echo "[*] Bucket 'loki-data' already exists. Skipping bucket creation."
else
    echo "[*] Creating bucket 'loki-data'..."
    HTTP_STATUS=$(curl -s -w "%{http_code}" -b $RUN_PATH/minio-cookie -XPOST \
        "http://127.0.0.1:9001/api/v1/buckets" \
        -H 'Accept: */*' \
        -H 'Content-Type: application/json' \
        -d '{"name":"loki-data","versioning":{"enabled":false,"excludePrefixes":[],"excludeFolders":false},"locking":false}' \
        -o /dev/null)

    if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
        echo "[*] Successfully created bucket 'loki-data'!"
    else
        echo "[!] Failed to create bucket 'loki-data'. HTTP Status: $HTTP_STATUS"
    fi
fi

echo "[*] Checking if user 'lokiuser' exists..."
USER_CHECK=$(curl -s -w "%{http_code}" -b $RUN_PATH/minio-cookie -XGET \
    "http://127.0.0.1:9001/api/v1/users" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json')

USER_HTTP_STATUS=$(echo "$USER_CHECK" | tail -n1)
USER_RESPONSE=$(echo "$USER_CHECK" | head -n -1)

if [[ $USER_HTTP_STATUS =~ ^2[0-9][0-9]$ ]] && echo "$USER_RESPONSE" | grep -q '"accessKey":"lokiuser"'; then
    echo "[*] User 'lokiuser' already exists. Skipping user creation."
else
    echo "[*] Creating user 'lokiuser'..."
    HTTP_STATUS=$(curl -s -w "%{http_code}" -b $RUN_PATH/minio-cookie -XPOST \
        "http://127.0.0.1:9001/api/v1/users" \
        -H 'Accept: */*' \
        -H 'Content-Type: application/json' \
        -H 'Origin: http://127.0.0.1:9001' \
        -d '{"accessKey":"'$LOKI_USER'","secretKey":"'$LOKI_USER_PASSWORD'","groups":[],"policies":["readwrite"]}' \
        -o /dev/null)

    if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
        echo "[*] Successfully created user 'lokiuser'!"
    else
        echo "[!] Failed to create user 'lokiuser'. HTTP Status: $HTTP_STATUS"
    fi
fi

echo "[*] Checking if service account credentials for lokiuser exist..."
SA_CHECK=$(curl -s -w "%{http_code}" -b $RUN_PATH/minio-cookie -XGET \
    "http://127.0.0.1:9001/api/v1/user/lokiuser/service-accounts" \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json')

SA_HTTP_STATUS=$(echo "$SA_CHECK" | tail -n1)
SA_RESPONSE=$(echo "$SA_CHECK" | head -n -1)

if [[ $SA_HTTP_STATUS =~ ^2[0-9][0-9]$ ]] && echo "$SA_RESPONSE" | grep -q '"accessKey":"'$LOKI_USER_ACCESS_KEY_ID'"'; then
    echo "[*] Service account credentials for lokiuser already exist. Skipping service account creation."
else
    echo "[*] Creating service account credentials for lokiuser..."
    HTTP_STATUS=$(curl -s -w "%{http_code}" -b $RUN_PATH/minio-cookie -XPOST \
        "http://127.0.0.1:9001/api/v1/user/lokiuser/service-account-credentials" \
        -H 'Accept: */*' \
        -H 'Content-Type: application/json' \
        -H 'Origin: http://127.0.0.1:9001' \
        -d '{"policy":"","accessKey":"'$LOKI_USER_ACCESS_KEY_ID'","secretKey":"'$LOKI_USER_SECRET_ACCESS_KEY'","description":"","comment":"","name":"","expiry":null}' \
        -o /dev/null)

    if [[ $HTTP_STATUS =~ ^2[0-9][0-9]$ ]]; then
        echo "[*] Successfully created service account credentials for lokiuser!"
    else
        echo "[!] Failed to create service account credentials for lokiuser. HTTP Status: $HTTP_STATUS"
    fi
fi

echo "[*] Deleting cookie..."
rm -f $RUN_PATH/minio-cookie

killall minio

/minio/bin/minio server /minio/data --console-address :9001
