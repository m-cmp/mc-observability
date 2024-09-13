FROM alpine AS builder
RUN touch /tmp/myfile
RUN wget https://dl.influxdata.com/telegraf/releases/telegraf-1.29.5_linux_amd64.tar.gz
RUN tar xfpz telegraf-1.29.5_linux_amd64.tar.gz ./telegraf-1.29.5/usr/bin/telegraf --strip-components 4 && mv telegraf mc-observability-agent-collector

FROM openjdk:17-slim AS prod
RUN mkdir -p /usr/bin/mc-observability-agent
RUN mkdir -p /etc/mc-observability-agent/conf
RUN apt-get update && apt-get install -y curl
COPY ./java-module/mc-o11y-agent/build/libs/mc-o11y-agent.jar mc-o11y-agent.jar
COPY --from=builder mc-observability-agent-collector /usr/bin/mc-observability-agent/mc-observability-agent-collector
COPY ./java-module/mc-o11y-manager/build/libs/mc-o11y-manager.jar mc-o11y-manager.jar
ENTRYPOINT ["sh", "-c", "java -jar mc-o11y-agent.jar & java -jar mc-o11y-manager.jar"]