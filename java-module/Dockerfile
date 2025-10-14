FROM openjdk:17-slim AS builder

RUN apt-get update \
    && apt-get install -y git make \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    apt-get clean

WORKDIR /java-module

COPY . .

RUN git config --global user.email "ish@innogrid.com"
RUN git config --global user.name "ish-hcc"
RUN git init
RUN git commit --allow-empty -m "a commit for the build"

RUN make

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.18.1/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

FROM eclipse-temurin:17-jre-alpine-3.22 AS prod

RUN apk add --no-cache curl

RUN mkdir -p /app-config

COPY --from=builder /java-module/mc-o11y-manager/build/libs/mc-o11y-manager.jar /mc-o11y-manager.jar
COPY --from=builder /java-module/mc-o11y-manager/mc-o11y-manager_default_key /mc-o11y-manager.key
COPY --from=builder /java-module/mc-o11y-manager/src/main/resources/application.yaml /app-config/application.yaml
COPY --from=builder /opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

RUN mkdir -p /config

COPY ./docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
