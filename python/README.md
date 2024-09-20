# mc-observability-insight

This repository provides a Monitoring system of M-CMP.

A sub-system of [M-CMP platform](https://github.com/m-cmp/docs/tree/main) to deploy and manage Multi-Cloud Infrastructures. 

## Overview

- The package of this repository is a multi-cloud integrated monitoring framework that provides integrated monitoring capabilities for larger-scale infrastructure services and Kubernetes(K8S) services in a heterogeneous cloud integration environment.
- It offers insights based on operational management information.
- Through integrated monitoring and operational management of multi-clouds, it avoids the complexity between different clouds and centralizes management, enabling stable and efficient system operation.
- The overall flow of the integrated system is as follows: Information, Metrics, events, and log details of the monitoring target are collected through the cloud API and agents installed on vitual servers or physical equipment.

## How to Use

### Development environment
  - MariaDB (10.7.6)
  - InfluxDB (1.8.10)
  - Python (3.11.9)
  - fastapi (0.97.0)
  - apache-airflow (2.8.4)

### 1. Install & Run Services
[Initialize mc-observability](# mc-observability-insight

This repository provides a Monitoring system of M-CMP.

A sub-system of [M-CMP platform](https://github.com/m-cmp/docs/tree/main) to deploy and manage Multi-Cloud Infrastructures. 

## Overview

- The package of this repository is a multi-cloud integrated monitoring framework that provides integrated monitoring capabilities for larger-scale infrastructure services and Kubernetes(K8S) services in a heterogeneous cloud integration environment.
- It offers insights based on operational management information.
- Through integrated monitoring and operational management of multi-clouds, it avoids the complexity between different clouds and centralizes management, enabling stable and efficient system operation.
- The overall flow of the integrated system is as follows: Information, Metrics, events, and log details of the monitoring target are collected through the cloud API and agents installed on vitual servers or physical equipment.\

## How to Use

### Development environment
  - MariaDB (10.7.6)
  - InfluxDB (1.8.10)
  - Python (3.11.9)
  - fastapi (0.97.0)
  - apache-airflow (2.8.4)

### 1. Install & Run Services
[Initialize mc-observability](https://github.com/m-cmp/mc-observability/tree/main/java-module#how-to-use)  
The installation process of the Insight module is included in the installation process of the mc-observability 
java-module (No. 1 to No. 7). Therefore, there is no need for a separate installation process.  

※ In order to use the Insight function, data collection of monitoring items used in the Insight function is required in
advance. Therefore, the process of installing the mc-observability agent and adding the plug-in must be preceded.

### 2. Insight Module API server swagger docs
You can check swagger-ui by connecting to '/docs' on port 9001 of mc-observability installation url.  

Ex: http://observability_VM_PUBLIC_IP:9001/docs

### 3. Insight Module Scheduler UI
You can check the scheduler UI by accessing port 9002 of the mc-observerability installation url.  
The initial connection account is "admin/admin".  

Ex: http://observability_VM_PUBLIC_IP:9002

### 4. API usage examples 
API can be used as shown in the example below.  
```
curl -X 'GET' \
  'http://observability_VM_PUBLIC_IP:9001/api/o11y/insight/anomaly-detection/options' \
  -H 'accept: application/json'
```

### 5. Insight Module API Usage Scenario
Detailed API usage scenarios are written in the link below.  
[API Usage Scenario](https://github.com/m-cmp/mc-observability/issues/31)
