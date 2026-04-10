#!/bin/bash

PREFIX="influx -host 127.0.0.1 -port $INFLUXDB_INIT_PORT -username ${INFLUXDB_ADMIN_USER} -password ${INFLUXDB_ADMIN_PASSWORD} -execute"

$PREFIX "CREATE DATABASE insight"
$PREFIX "CREATE DATABASE downsampling"
