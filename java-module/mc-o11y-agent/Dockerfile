FROM openjdk:17-slim AS prod
COPY ./build/libs/mc-o11y-agent.jar mc-o11y-agent.jar
ENTRYPOINT ["java", "-jar","mc-o11y-agent.jar"]