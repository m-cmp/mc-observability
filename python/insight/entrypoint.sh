#!/bin/bash
set -e

CREDENTIAL_KEY_FILE="${LLM_CREDENTIAL_KEY_FILE:-/mc-insight/secrets/llm_credential_key}"
if [ -z "${LLM_CREDENTIAL_KEY:-}" ]; then
    if [ ! -s "$CREDENTIAL_KEY_FILE" ]; then
        mkdir -p "$(dirname "$CREDENTIAL_KEY_FILE")"
        (umask 077; python -c 'from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())' > "$CREDENTIAL_KEY_FILE")
        chmod 600 "$CREDENTIAL_KEY_FILE"
    fi
    export LLM_CREDENTIAL_KEY="$(cat "$CREDENTIAL_KEY_FILE")"
fi

mkdir -p ./log
gunicorn --bind 0.0.0.0:9001 --workers 20 --threads 20 main:app --log-config config/log.ini --worker-class uvicorn.workers.UvicornH11Worker --preload --timeout 60
