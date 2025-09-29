FROM openjdk:17-slim AS builder

RUN apt-get update \
    && apt-get install -y make git \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    apt-get clean

WORKDIR /java-module

COPY . .

RUN git config --global user.email "ish@innogrid.com"
RUN git config --global user.name "ish-hcc"
RUN git init
RUN git commit --allow-empty -m "a commit for the build"

RUN make

FROM openjdk:17-slim AS prod

RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    apt-get clean

RUN curl -fsL -o /opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.18.1/opentelemetry-javaagent.jar

COPY --from=builder /java-module/mc-o11y-manager/build/libs/mc-o11y-manager.jar /mc-o11y-manager.jar
COPY --from=builder /java-module/mc-o11y-manager/mc-o11y-manager_default_key /mc-o11y-manager.key
RUN mkdir -p /config

ENTRYPOINT ["sh", "-c", "java -jar /mc-o11y-manager.jar"]
