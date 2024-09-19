#!/bin/bash
mkdir -p ./log
gunicorn --bind 0.0.0.0:9001 --workers 20 --threads 20 main:app --log-config config/log.ini --worker-class uvicorn.workers.UvicornH11Worker --preload --timeout 60