# mc-observability
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability?ref=badge_shield)


This repository provides a Monitoring system of M-CMP.

A sub-system of [M-CMP platform](https://github.com/m-cmp/docs/tree/main) to deploy and manage Multi-Cloud Infrastructures. 

## Overview

- The package of this repository is a multi-cloud integrated monitoring framework that provides integrated monitoring capabilities for larger-scale infrastructure services and Kubernetes(K8S) services in a heterogeneous cloud integration environment.
- It offers insights based on operational management information.
- Through integrated monitoring and operational management of multi-clouds, it avoids the complexity between different clouds and centralizes management, enabling stable and efficient system operation.
- The overall flow of the integrated system is as follows: Information, Metrics, events, and log details of the monitoring target are collected through the cloud API and agents installed on vitual servers or physical equipment.\

### System architecture
<details>
<summary>접기/펼치기</summary>

```mermaid
C4Context
Enterprise_Boundary(boundary0, "M-CMP") {
  Person(customer0, "M-CMP User")
  Boundary(boundary1, "M-CMP Observability") {
    Container(container0, "M-CMP Observability Manager")
    Deployment_Node(deploy10, "Azure") {
      Deployment_Node(deploy11, "VM 1") {
        Container(container11, "M-CMP Observability Agent Manager")
      }
      Deployment_Node(deploy12, "VM 2") {
        Container(container12, "M-CMP Observability Agent")
      }
      Deployment_Node(deploy13, "VM 3") {
        Container(container13, "M-CMP Observability Agent")
      }
      ContainerDb(database11, "MariaDB")
      ContainerDb(database12, "InfluxDB")
    }
    Deployment_Node(deploy20, "AWS") {
      Deployment_Node(deploy21, "VM 1") {
        Container(container21, "M-CMP Observability Agent Manager")
      }
      Deployment_Node(deploy22, "VM 2") {
        Container(container22, "M-CMP Observability Agent")
      }
      Deployment_Node(deploy23, "VM 3") {
        Container(container23, "M-CMP Observability Agent")
      }
      ContainerDb(database21, "MariaDB")
      ContainerDb(database22, "InfluxDB")
    }
    Deployment_Node(deploy30, "Openstack") {
      Deployment_Node(deploy31, "VM 1") {
        Container(container31, "M-CMP Observability Agent Manager")
      }
      Deployment_Node(deploy32, "VM 2") {
        Container(container32, "M-CMP Observability Agent")
      }
      Deployment_Node(deploy33, "VM 3") {
        Container(container33, "M-CMP Observability Agent")
      }
      ContainerDb(database31, "MariaDB")
      ContainerDb(database32, "InfluxDB")
    }
    BiRel(container0, container11, "REST API")
    BiRel(container0, container21, "")
    BiRel(container0, container31, "")
  }
}
BiRel(customer0, container0, "REST API")
UpdateRelStyle(customer0, container0, $offsetY="-50")
UpdateRelStyle(container0, container11, $offsetY="-150")
```

```mermaid
C4Context
Deployment_Node(deploy10, "Cloud Service Provider") {
  Deployment_Node(deploy11, "VM 1") {
    Container(container11, "M-CMP Observability Agent Manager")
  }
  Deployment_Node(deploy12, "VM 2") {
    Container(container12, "M-CMP Observability Agent")
  }
  Deployment_Node(deploy13, "VM 3") {
    Container(container13, "M-CMP Observability Agent")
  }
  ContainerDb(database11, "MariaDB", "Monitoring config Database")
  ContainerDb(database12, "InfluxDB", "Metrics Database")
  BiRel(container11, database11, "")
  BiRel(container12, database11, "")
  BiRel(container13, database11, "")
  Rel(database12, container11, "")
  Rel(container12, database12, "")
  Rel(container13, database12, "")
}
UpdateRelStyle(container11, database11, $lineColor="orange")
UpdateRelStyle(container12, database11, $lineColor="orange")
UpdateRelStyle(container13, database11, $lineColor="orange")

UpdateRelStyle(database12, container11, $lineColor="green")
UpdateRelStyle(container12, database12, $lineColor="green")
UpdateRelStyle(container13, database12, $lineColor="green")
```
</details>

## How to Use

### Development environment
  - MariaDB (10.7.6)
  - InfluxDB (1.8.10)
  - Chronograf (1.10)
  - Telegraf (1.26.1)
  - SpringBoot (2.7.6)
  - Java (17)

### Step one: clone source
```
$ git clone https://github.com/m-cmp/mc-observability.git ${YourFolderName}
```

### Step two: Go to Java folder
```
$ cd ${YourFolderName}/java
```

### Step three: Subsystem docker-compose run
```
$ docker-compose up -d
```

### Step four: Network check
```
$ netstat -lntp
# Active Internet connections (only servers)
# Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name
# tcp        0      0 0.0.0.0:8888            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:3306            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:8086            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::8888                 :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::3306                 :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::8086                 :::*                    LISTEN      ${YourPID}/docker-proxy
```

### Step five: Run install script
```
$ script/install.sh
```

### Swagger Docs
https://m-cmp.github.io/mc-observability/java/swagger

#### [v0.3.0 swagger api preview](https://m-cmp.github.io/mc-observability/java/swagger/index_copy.html)

### API Use guide (swagger docs linked mermaid contents)
#### Observability Monitoring target setting guide

<details>
<summary>접기/펼치기</summary>

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
</details>

## How to Contribute

- Issues/Discussions/Ideas: Utilize issue of mc-observability


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability?ref=badge_large)