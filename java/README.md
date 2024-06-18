# mc-observability
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability?ref=badge_shield)


This repository provides a Monitoring system of M-CMP.

A sub-system of [M-CMP platform](https://github.com/m-cmp/docs/tree/main) to deploy and manage Multi-Cloud Infrastructures. 

## Overview

- The package of this repository is a multi-cloud integrated monitoring framework that provides integrated monitoring capabilities for larger-scale infrastructure services and Kubernetes(K8S) services in a heterogeneous cloud integration environment.
- It offers insights based on operational management information.
- Through integrated monitoring and operational management of multi-clouds, it avoids the complexity between different clouds and centralizes management, enabling stable and efficient system operation.
- The overall flow of the integrated system is as follows: Information, Metrics, events, and log details of the monitoring target are collected through the cloud API and agents installed on vitual servers or physical equipment.

## How to Use

### Development environment
  - SpringBoot (2.7.6)
  - MariaDB (10.7.6)
  - InfluxDB (1.8.10)
  - Telegraf (1.26.1)
  - Java (17)

### Use guide
#### Observability Monitoring target setting guide

```mermaid
flowchart TD
A([Start]) --> B[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/host-controller/listUsingGET'>Get&Select monitoring host</a>]
B --> C{Select<br>Item/Storage}
C --Item--> D[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/system-controller/getPluginsUsingGET'>Get&Select item plugin</a>]
D --> E[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/host-item-controller/createUsingPOST'>Set monitoring item</a>]
E --> F{Add more?}
F --Y--> D
F --N--> Z([End])
C --Storage--> G[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/system-controller/getPluginUsingGET'>Get&Select storage plugin</a>]
G --> H[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/host-storage-controller/createUsingPOST_1'>Set monitoring storage</a>]
H --> I{Add more?}
I --Y--> G
I --N--> Z([End])
```

#### Observability Metrics view guide

```mermaid
flowchart TD
A([Start]) --> B[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/influx-db-controller/getListUsingGET'>Get&Select Integration InfluxDB</a>]
B --> C[Get field/tag]
C --Fields--> D[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/influx-db-controller/getMeasurementAndFieldsUsingGET'>Get field list</a><br>Field is metric target]
C --Tags--> E[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/influx-db-controller/getTagsUsingGET'>Get tag list</a><br>Tag is metric filter]
D --> F[<a href='https://m-cmp.github.io/mc-observability/java/swagger/#/metric-controller/getMetricUsingGET'>Get metrics</a>]
E --> F
```

#### M-CMP Observability monitoring setting sequenceDiagram

```mermaid
sequenceDiagram
participant M-CMP User
participant MC-O11y-Manager
participant MC-O11y-Agent-Manager
participant MariaDB
participant MC-O11y-Agent
M-CMP User ->> MC-O11y-Manager: Request all O11y-Agent-Manager
MC-O11y-Manager ->> M-CMP User: Send all O11y-Agent-Manager list
M-CMP User ->> M-CMP User: Select O11y-Agent-Manager

loop Configuration Changes
  Note over MariaDB,MC-O11y-Agent: Scheduler section
  MC-O11y-Agent ->> MariaDB: Request all changed config
  MariaDB ->> MC-O11y-Agent: Send changed config list
  alt one or more changed?
    MC-O11y-Agent ->> MC-O11y-Agent: Config file update
  else
    Note over MC-O11y-Agent: sleep
  end
end

M-CMP User ->> MC-O11y-Manager: Request all monitoring plugin
loop Monitoring setting
  Note over M-CMP User, MariaDB: Monitoring plugin setting<br>plugin is item+storage mean
  M-CMP User ->> MC-O11y-Manager: Request all monitoring plugin
  MC-O11y-Manager ->> MC-O11y-Agent-Manager: Request forwarding
  MC-O11y-Agent-Manager ->> MariaDB: Request forwarding
  MariaDB ->> MC-O11y-Agent-Manager: Send all monitoring plugin list
  MC-O11y-Agent-Manager ->> MC-O11y-Manager: Response forwarding
  MC-O11y-Manager ->> M-CMP User: Response forwarding
  M-CMP User ->> MC-O11y-Manager: Add/Update monitoring plugin
  MC-O11y-Manager ->> MC-O11y-Agent-Manager: Request forwarding
  MC-O11y-Agent-Manager ->> MariaDB: Request forwarding
  MariaDB ->> MC-O11y-Agent-Manager: Return Insert or Update result
  MC-O11y-Agent-Manager ->> MC-O11y-Manager: Response forwarding
  MC-O11y-Manager ->> M-CMP User: Response forwarding
end
```

### Use guide & basic scenario Full [ppt](./M-CMP%20Agent%20Use%20guide.ppt) / [pdf](./M-CMP%20Agent%20Use%20guide%2020240531.pdf)

### [API Docs yaml](./swagger.yaml)

## How to Contribute

- Issues/Discussions/Ideas: Utilize issue of mc-observability


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability?ref=badge_large)