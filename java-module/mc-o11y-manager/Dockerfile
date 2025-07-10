FROM openjdk:21-slim

RUN apt-get update \
    && apt-get install -y curl git \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    apt-get clean

COPY ./build/libs/mc-o11y-manager.jar /mc-o11y-manager.jar
COPY ./o11y-manager_default_key /o11y-manager.key
RUN mkdir -p /config

ENTRYPOINT ["sh", "-c", "java -jar /o11y-manager.jar"]
