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
  - Telegraf (1.29.5)
  - SpringBoot (2.7.6)
  - Java (17)

### Step one: clone source
```
$ git clone https://github.com/m-cmp/mc-observability.git ${YourFolderName}
```

### Step two: Go to Java folder
```
$ cd ${YourFolderName}/java-module
```

### Step three: set .env (edit .env)
```
$ cp .env.sample .env
```

### Step four: Subsystem docker-compose run
```
$ docker compose up -d
```

### Step five: Network check
```
$ netstat -lntp
# Active Internet connections (only servers)
# Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name
# tcp        0      0 0.0.0.0:18080           0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:18081           0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:3306            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:5601            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:8086            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:8888            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:9200            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp        0      0 0.0.0.0:9600            0.0.0.0:*               LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::18080                :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::18081                :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::3306                 :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::5601                 :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::8086                 :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::8888                 :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::9200                 :::*                    LISTEN      ${YourPID}/docker-proxy
# tcp6       0      0 :::9600                 :::*                    LISTEN      ${YourPID}/docker-proxy
```

### Swagger Docs
#### [v0.3.0 swagger api](https://m-cmp.github.io/mc-observability/java-module/swagger/index.html)

### API Use guide (swagger docs linked mermaid contents)
#### Observability Monitoring target setting guide

```mermaid
sequenceDiagram
participant M-CMP User
participant MC-O11y-Manager
participant MC-O11y-Agent-Manager
participant CB-Tumblebug
participant MariaDB
participant MC-O11y-Agent

Note over M-CMP User: run docker-image
M-CMP User ->> MC-O11y-Manager: Request install agent<br>param: nsId, targetId
MC-O11y-Manager ->> MC-O11y-Agent-Manager: Check installed target
alt Already install target?
  MC-O11y-Agent-Manager ->> MC-O11y-Manager: Response installed
  MC-O11y-Manager ->> M-CMP User: Response installed
else
  MC-O11y-Agent-Manager ->> CB-Tumblebug: Request target connection info
  CB-Tumblebug ->> MC-O11y-Agent-Manager: Response connection info
  MC-O11y-Agent-Manager ->> MC-O11y-Agent: Connect target
  MC-O11y-Agent-Manager ->> MC-O11y-Agent: Install agent
  MC-O11y-Agent ->> MariaDB: Regist managed target
  MC-O11y-Agent ->> MC-O11y-Agent-Manager: Installation complete
  MC-O11y-Agent-Manager ->> MC-O11y-Manager: Installation complete
  MC-O11y-Manager ->> M-CMP User: Installation complete
end
```

## How to Contribute

- Issues/Discussions/Ideas: Utilize issue of mc-observability


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fm-cmp%2Fmc-observability?ref=badge_large)