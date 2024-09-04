FROM alpine AS builder
RUN touch /tmp/myfile
RUN wget https://dl.influxdata.com/telegraf/releases/telegraf-1.26.1_linux_amd64.tar.gz
RUN tar xfpz telegraf-1.26.1_linux_amd64.tar.gz ./telegraf-1.26.1/usr/bin/telegraf --strip-components 4 && mv telegraf mc-observability-agent-collector

FROM openjdk:17-slim AS prod
RUN mkdir -p /usr/bin/mc-observability-agent
RUN mkdir -p /etc/mc-observability-agent/conf
COPY java-module/mc-o11y-agent/build/libs/mc-o11y-agent.jar /usr/bin/mc-observability-agent/mc-o11y-agent.jar
COPY --from=builder mc-observability-agent-collector /usr/bin/mc-observability-agent/mc-observability-agent-collector
ENTRYPOINT ["java", "-jar","/usr/bin/mc-observability-agent/mc-o11y-agent.jar"]