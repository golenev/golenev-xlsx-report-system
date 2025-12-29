# Руководство по запуску Test Report System

Инструкция по сборке и развёртыванию SPA + Spring Boot backend в контейнерах или локально.

## Предварительные требования
- **Docker + Docker Compose.** Достаточно, чтобы собрать и запустить всё приложение без установки Java и Node.js на хосте.
- **Java 17+, Gradle 8.x, Node.js 18+ с npm 9+.** Нужны только для локальной разработки без Docker.

## Быстрый старт (единый контейнер)
Соберите фронтенд и backend в один образ и поднимите вместе с PostgreSQL из корня репозитория:

```bash
docker compose up -d
```

Что произойдёт:
- Сформируется единый образ `app`, где Spring Boot отдаёт API и статический фронтенд.
- Поднимется PostgreSQL на порту `55432` с данными в volume `db-data`.
- Приложение будет доступно по адресу `http://localhost:18080` (фронт и API на одном порту).

Остановка сервисов:
```bash
docker compose down
```
Для удаления данных базы выполните `docker compose down -v`.

## Обновление кода в запущенном контейнере
После правок в backend или фронтенде пересоберите образ и перезапустите сервисы:

```bash
docker compose up -d --build
```

Команда соберёт новый образ `app` и перезапустит контейнер без трогания volumes.

## Проверка фичи из отдельной ветки
1. Переключитесь на нужную ветку:
   ```bash
   git fetch
   git checkout feature/my-branch
   git pull
   ```
2. Пересоберите образы и перезапустите сервисы (образ соберётся из текущей ветки):
   ```bash
   docker compose up -d --build
   ```

При необходимости перед пересборкой остановите прошлый вариант: `docker compose down`.

## Локальная разработка с hot-reload
Оставьте базу в Docker, а приложения запустите из исходников:
1. Поднять только БД:
   ```bash
   docker compose up -d db
   ```
2. Backend:
   ```bash
   cd backend
   ./gradlew bootRun
   ```
3. Frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Vite поднимет dev-сервер на `http://localhost:5173` и будет проксировать `/api` на `http://localhost:18080`.

## Использование собственной PostgreSQL
Можно пропустить запуск контейнера `db` и настроить подключение к внешней базе. Перед стартом backend укажите переменные окружения `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Назначение Dockerfile'ов
- **Dockerfile (корень)** — собирает фронтенд, прокладывает статические файлы в Spring Boot JAR и запускает единый контейнер с API и SPA на порту `18080`.
- **backend/Dockerfile** — многоэтапная сборка для отдельного деплоя backend без фронтенда.
- **frontend/Dockerfile** — сборка production-бандла Vite для отдельного контейнера SPA.

## Переменные окружения backend
| Переменная | Назначение | Значение по умолчанию |
|------------|------------|------------------------|
| `DB_URL` | JDBC-строка подключения к PostgreSQL | `jdbc:postgresql://localhost:55432/test_report` |
| `DB_USERNAME` | Пользователь PostgreSQL | `report` |
| `DB_PASSWORD` | Пароль PostgreSQL | `report` |
| `COLUMN_CONFIG_PATH` | Путь к JSON с ширинами колонок | `classpath:config/column-config.json` |
| `PORT` | Порт HTTP-сервера Spring Boot | `18080` |

## Переменные окружения фронтенда
Для dev-режима Vite можно указать базовый URL API:
```bash
VITE_API_BASE_URL="http://example.com:18080" npm run dev
```
