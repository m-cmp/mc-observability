# mc-observability-insight

This repository provides a Monitoring system of M-CMP.

A sub-system of [M-CMP platform](https://github.com/m-cmp/docs/tree/main) to deploy and manage Multi-Cloud Infrastructures.

## Overview

- The package of this repository is a multi-cloud integrated monitoring framework that provides integrated monitoring capabilities for larger-scale infrastructure services and Kubernetes(K8S) services in a heterogeneous cloud integration environment.
- It offers insights based on operational management information.
- Through integrated monitoring and operational management of multi-clouds, it avoids the complexity between different clouds and centralizes management, enabling stable and efficient system operation.
- The overall flow of the integrated system is as follows: Information, Metrics, events, and log details of the monitoring target are collected through the cloud API and agents installed on virtual servers or physical equipment.

## How to Use

### Development Environment
  - MariaDB (10.7.6)
  - InfluxDB (1.8.10)
  - Python (3.12)
  - FastAPI (0.111.0)
  - Apache Airflow (2.8.4)

### 1. Install & Run Services
[Initialize mc-observability](https://github.com/m-cmp/mc-observability/tree/main/java-module#how-to-use)
The installation process of the Insight module is included in the installation process of the mc-observability
java-module (No. 1 to No. 7). Therefore, there is no need for a separate installation process.

**Note**: In order to use the Insight function, data collection of monitoring items used in the Insight function is required in advance. Therefore, the process of installing the mc-observability agent and adding the plug-in must be preceded.

### 2. Insight Module API Documentation
You can access the Swagger UI by connecting to `/docs` on port 9001 of the mc-observability installation URL.

Example: `http://observability_VM_PUBLIC_IP:9001/docs`

### 3. Insight Module Scheduler UI
You can access the scheduler UI on port 9002 of the mc-observability installation URL.
The initial connection account is `admin/admin`.

Example: `http://observability_VM_PUBLIC_IP:9002`

### 4. API Usage Examples

#### Anomaly Detection
```bash
# Get anomaly detection options
curl -X 'GET' \
  'http://observability_VM_PUBLIC_IP:18080/api/o11y/insight/anomaly-detection/options' \
  -H 'accept: application/json'

# Get anomaly history for MCI
curl -X 'GET' \
  'http://observability_VM_PUBLIC_IP:18080/api/o11y/insight/anomaly-detection/ns/{nsId}/mci/{mciId}/history?measurement=cpu' \
  -H 'accept: application/json'

# Get anomaly history for VM
curl -X 'GET' \
  'http://observability_VM_PUBLIC_IP:18080/api/o11y/insight/anomaly-detection/ns/{nsId}/mci/{mciId}/vm/{vmId}/history?measurement=cpu' \
  -H 'accept: application/json'
```

#### Prediction
```bash
# Get prediction options
curl -X 'GET' \
  'http://observability_VM_PUBLIC_IP:18080/api/o11y/insight/predictions/options' \
  -H 'accept: application/json'

# Predict monitoring data for MCI
curl -X 'POST' \
  'http://observability_VM_PUBLIC_IP:18080/api/o11y/insight/predictions/ns/{nsId}/mci/{mciId}' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "measurement": "cpu",
    "field": "usage_idle",
    "range": "24h"
  }'

# Predict monitoring data for VM
curl -X 'POST' \
  'http://observability_VM_PUBLIC_IP:18080/api/o11y/insight/predictions/ns/{nsId}/mci/{mciId}/vm/{vmId}' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "measurement": "cpu",
    "field": "usage_idle",
    "range": "24h"
  }'
```

#### Log & Alert Analysis
```bash
# Query log analysis
curl -X 'POST' \
  'http://observability_VM_PUBLIC_IP:18080/api/o11y/insight/log-analysis/query' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "your log analysis query"
  }'

# Query alert analysis
curl -X 'POST' \
  'http://observability_VM_PUBLIC_IP:18080/api/o11y/insight/alert-analysis/query' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "your alert analysis query"
  }'
```

### 5. Insight Module API Usage Scenario
Detailed API usage scenarios are written in the link below.
[API Usage Scenario](https://github.com/m-cmp/mc-observability/issues/31)
