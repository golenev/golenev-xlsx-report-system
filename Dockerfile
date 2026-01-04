# Сборка фронтенда
FROM node:18 AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
# Use npm install instead of npm ci because the lockfile may lag behind the
# manifest when new frontend dependencies are added (e.g., chart.js, TypeScript
# typings). npm install will refresh the lockfile during the image build and
# avoid hard failures from a temporarily out-of-sync package-lock.
RUN npm install
COPY frontend/ .
RUN npm run build

# Сборка backend с включением статических файлов фронтенда
FROM gradle:8.5-jdk17 AS backend-build
WORKDIR /workspace
COPY backend backend
COPY allure-helpers allure-helpers
COPY config config
COPY --from=frontend-build /frontend/dist ./backend/src/main/resources/static
WORKDIR /workspace/backend
RUN gradle clean bootJar

# Финальный образ с единым приложением
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV DB_URL=jdbc:postgresql://db:5432/test_report \
    DB_USERNAME=report \
    DB_PASSWORD=report \
    COLUMN_CONFIG_PATH=classpath:config/column-config.json \
    PORT=18080
COPY --from=backend-build /workspace/backend/build/libs/*.jar app.jar
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
