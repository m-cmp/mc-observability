FROM openjdk:17-slim AS prod
COPY ./java-module/mc-o11y-manager/build/libs/mc-o11y-manager.jar mc-o11y-manager.jar
ENTRYPOINT ["java", "-jar","mc-o11y-manager.jar"]