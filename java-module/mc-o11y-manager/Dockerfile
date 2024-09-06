FROM openjdk:17-slim AS prod
RUN apt-get update && apt-get install -y curl
COPY ./java-module/mc-o11y-manager/build/libs/mc-o11y-manager.jar mc-o11y-manager.jar
ENTRYPOINT ["java", "-jar","mc-o11y-manager.jar"]