FROM gradle:8.5-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle clean bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Europe/Moscow \
    APP_TIME_ZONE=Europe/Moscow \
    JAVA_TOOL_OPTIONS=-Duser.timezone=Europe/Moscow
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
