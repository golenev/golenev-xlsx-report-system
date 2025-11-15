# Test Report System

Полнофункциональная система ведения регрессионных тестов с web-интерфейсом в стиле Google Sheets.

## Состав проекта

- **backend/** — Kotlin + Spring Boot, PostgreSQL, Flyway, экспорт Excel
- **frontend/** — React SPA (Vite)
- **config/column-config.json** — конфигурация фиксированных ширин колонок
- **docker-compose.yml** — быстрый запуск всех сервисов и PostgreSQL

## Подготовка окружения

Необходимые инструменты:

- Java 17+
- Kotlin (идёт вместе с Gradle)
- Gradle 8.x (если нет, установите с https://gradle.org/install)
- Node.js 18+
- npm 9+
- Docker + Docker Compose (для контейнерного запуска)

## Backend

### Локальный запуск

```bash
cd backend
# Применение миграций и запуск dev-сервера
gradle bootRun
```

Переменные окружения:

- `DB_URL` — JDBC строка (по умолчанию `jdbc:postgresql://localhost:5432/test_report`)
- `DB_USERNAME`, `DB_PASSWORD` — учётные данные PostgreSQL (по умолчанию `report/report`)
- `COLUMN_CONFIG_PATH` — путь к JSON с ширинами колонок (`classpath:config/column-config.json`)
- `PORT` — порт приложения (по умолчанию 8080)

### PostgreSQL и миграции

1. Поднимите PostgreSQL (локально или через Docker):
   ```bash
   docker run --name test-report-db -e POSTGRES_DB=test_report -e POSTGRES_USER=report -e POSTGRES_PASSWORD=report -p 5432:5432 -d postgres:15
   ```
2. При старте backend Flyway автоматически выполнит миграции из `backend/src/main/resources/db/migration`.

### Docker-образ backend

```bash
cd backend
docker build -t test-report-backend .
```

Контейнер принимает те же переменные окружения для подключения к БД.

## Frontend

### Настройка и запуск dev-сервера

```bash
cd frontend
npm install
npm run dev
```

Vite-прокси автоматически перенаправит запросы `/api` на `http://localhost:8080`.

### Production-сборка

```bash
npm run build
```

Результат появится в `frontend/dist`. Для предпросмотра:

```bash
npm run preview
```

### Конфигурация колонок

SPA получает ширины колонок вместе с данными из backend (`/api/tests`). Сам JSON лежит в `config/column-config.json`. Чтобы изменить значения:

1. Обновите `config/column-config.json`
2. При необходимости обновите `backend/src/main/resources/config/column-config.json`
3. Перезапустите backend (конфиг перечитывается при старте или через REST `GET /api/config/columns`)

## Совместный запуск через Docker Compose

```bash
docker compose up --build
```

Сервисы:

- PostgreSQL: `localhost:5432`
- Backend API: `http://localhost:8080`
- Frontend SPA: `http://localhost:5173`

Для остановки и очистки данных:

```bash
docker compose down -v
```

## REST API

### Получить данные таблицы

```
GET /api/tests
```

Ответ:

```json
{
  "items": [
    {
      "testId": "TC-001",
      "category": "Delivery",
      "shortTitle": "Успешная авторизация",
      "issueLink": "https://youtrack/.../ABC-1",
      "readyDate": "2025-11-15",
      "generalStatus": "Ready",
      "scenario": "Detailed test steps…",
      "notes": "any info",
      "runStatuses": ["PASSED", "FAILED", null, null, null],
      "updatedAt": "2024-04-01T12:15:00Z"
    }
  ],
  "runs": [
    { "runIndex": 1, "runDate": "2024-03-30" }
  ],
  "columnConfig": {
    "testId": 180
  }
}
```

### Пакетный upsert автотестов

```
POST /api/tests/batch
Content-Type: application/json
```

Тело запроса:

```json
{
  "items": [
    {
      "testId": "TC-001",
      "category": "Delivery",
      "shortTitle": "Успешная авторизация",
      "issueLink": "https://youtrack/.../ABC-1",
      "readyDate": "2025-11-15",
      "generalStatus": "Ready",
      "scenario": "Detailed test steps…",
      "notes": "any info",
      "runIndex": 3,
      "runStatus": "PASSED",
      "runDate": "2025-11-16"
    }
  ]
}
```

- `testId` — уникальный ключ. Если запись существует, поля обновляются (идемпотентно).
- `runIndex` опционально (1–5). Если указан, статус и дата прогона обновляются.
- Поля, отсутствующие в запросе, остаются без изменений.

### Частичное обновление из UI

```
PATCH /api/tests/{testId}
Content-Type: application/json
```

Пример тела:

```json
{
  "generalStatus": "In Progress"
}
```

Для обновления результата прогона вручную:

```json
{
  "runIndex": 2,
  "runStatus": "FAILED"
}
```

### Экспорт Excel

```
GET /api/tests/export/excel
```

Возвращает файл `.xlsx`, структура соответствует UI.

## Интеграция с автотестами

Используйте endpoint `/api/tests/batch` для пакетной передачи результатов.

- Подготавливайте батч по уникальным `testId`
- Указывайте только изменившиеся поля
- Для обновления статуса прогона передавайте `runIndex` (1–5) и `runStatus`
- `runDate` опционален. Если не указать, дата автоматически проставляется текущей

### Пример Kotlin-клиента (Spring WebClient)

```kotlin
import org.springframework.web.reactive.function.client.WebClient

data class TestUpsertRequest(
    val items: List<TestItem>
)

data class TestItem(
    val testId: String,
    val category: String? = null,
    val shortTitle: String? = null,
    val runIndex: Int? = null,
    val runStatus: String? = null
)

val client = WebClient.builder()
    .baseUrl("http://localhost:8080/api")
    .build()

suspend fun sendResults() {
    val payload = TestUpsertRequest(
        items = listOf(
            TestItem(testId = "TC-001", runIndex = 1, runStatus = "PASSED"),
            TestItem(testId = "TC-002", category = "Payments")
        )
    )

    client.post()
        .uri("/tests/batch")
        .bodyValue(payload)
        .retrieve()
        .toBodilessEntity()
        .awaitBody()
}
```

## UI особенности

- Таблица стилизована под Google Sheets: сетка, нумерация строк, буквенные заголовки
- Фиксированные ширины колонок из конфигурации
- Inline-редактирование: поля сохраняются по `blur`
- Выпадающие списки для Run Result N (PASSED/FAILED/NOT RUN)
- Автосохранение и отображение статуса «Saving…»
- Кнопка экспорта «Export to Excel»

## Полезные команды

- Запуск backend-тестов: `cd backend && gradle test`
- Линт фронтенда: `cd frontend && npm run lint`
- Сброс БД в Docker Compose: `docker compose down -v`

## Структура БД

```sql
CREATE TABLE test_report (
    id          bigserial PRIMARY KEY,
    test_id     text NOT NULL UNIQUE,
    category    text,
    short_title text,
    issue_link  text,
    ready_date  date,
    general_status text,
    scenario    text,
    notes       text,
    run_1_status text,
    run_2_status text,
    run_3_status text,
    run_4_status text,
    run_5_status text,
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE test_run_metadata (
    run_index int PRIMARY KEY,
    run_date  date
);
```

Готово к запуску «из коробки» по инструкциям выше.
