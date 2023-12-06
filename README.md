# mc-observability

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
  - Telegraf (1.28.5)
  - Java (1.8)

### Get Sourcecode
```bash
git clone https://github.com/m-cmp/mc-observability.git
```

### Download Telegraf Binary
- [Telegraf (Linux 64)](https://dl.influxdata.com/telegraf/releases/telegraf-1.28.5_linux_amd64.tar.gz)
```bash
tar xf telegraf-1.28.5_linux_amd64.tar.gz
mv telegraf-1.28.5/usr/bin/telegraf ./
```

### Build project
```bash
mvn clean install
```
### File composition

1. File Tree (M-CMP Observability Agent Manager)
```
-/ {{any directory}}
└ - m-cmp-agent.jar
└ - m-cmp-agent.conf
└ - application-{{profile}}.yaml
```
- m-cmp-agent.conf
```
RUN_ARGS="--spring.profiles.active=api,{{profile}}"
```
- application-{{profile}}.yaml
```
# Required
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://{{ip}}:{{port}}/{{databaseName}}?{{options}}
    username: {{username}}
    password: {{password}}
```

2. File Tree (M-CMP Observability Agent)
```
-/ {{any directory}}
└ - m-cmp-agent.jar
└ - m-cmp-agent.conf
└ - application-{{profile}}.yaml
└ - telegraf (Download Binary)
```
- m-cmp-agent.conf
```
RUN_ARGS="--spring.profiles.active={{profile}}"
```
- application-{{profile}}.yaml
```
# Required
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://{{ip}}:{{port}}/{{databaseName}}?{{options}}
    username: {{username}}
    password: {{password}}

# Optional
scheduler:
  expression:
    health-check: "*/5 * * * * ?" # Collector HealthCheck Scheduler cron expression
    config-check: "*/30 * * * * ?" # Collector config updater Scheduler cron expression
```

### Run Jar
```bash
./m-cmp-agent.jar start &
```

#### [API Postman Example](./m-cmp-observability-agent.postman_collection.json)

#### [API Docs](https://petstore.swagger.io/?url=http://github.com/hyeon-inno/mc-observability/blob/initialize/swagger.yaml)

## How to Contribute

- Issues/Discussions/Ideas: Utilize issue of mc-observability
