# Test Report System

Полнофункциональная система ведения регрессионных тестов с web-интерфейсом в стиле Google Sheets.

## Состав проекта

- **backend/** — Kotlin + Spring Boot, PostgreSQL, Flyway, экспорт Excel
- **frontend/** — React SPA (Vite)
- **config/column-config.json** — конфигурация фиксированных ширин колонок
- **docker-compose.yml** — единая точка запуска инфраструктуры и сервисов

## Необходимые инструменты

- Docker + Docker Compose — достаточно, чтобы собрать и запустить всё приложение без установки Java и Node.js на хостовую машину.
- Java 17+, Gradle 8.x и Node.js 18+ с npm 9+ нужны только если планируется локальная разработка без Docker.

## Быстрый запуск всего решения (единый контейнер)

Команда ниже собирает фронтенд и backend в один образ и поднимает его вместе с PostgreSQL. Выполняйте из корня репозитория:

```bash
docker compose up -d
```

Что произойдёт:
- Соберётся единый образ `app`, внутри которого Spring Boot отдаёт API и собранный фронтенд (статические файлы прокладываются в jar на этапе сборки).
- Поднимется база PostgreSQL на порту `55432` с сохранением данных в volume `db-data`.
- Приложение будет доступно по адресу `http://localhost:18080` (фронт и API на одном порту).

Остановить сервисы можно командой:
```bash
docker compose down
```
Если нужно удалить данные базы, используйте `docker compose down -v`.

### Как применить изменения логики в контейнере

После любых правок в коде (backend или frontend), чтобы они попали в запущенный контейнер,
пересоберите образ и перезапустите сервис:

```bash
docker compose up -d --build
```

Эта команда соберёт новый образ `app` и плавно перезапустит контейнер с обновлённой логикой.
Удалять volume с базой данных не требуется, если не менялась схема БД.

### Как протестировать фичу из конкретной ветки

Если фича лежит в отдельной ветке (например, в PR), для проверки достаточно переключиться на неё и пересобрать контейнеры:

1. Обновите локальный код нужной ветки:
   ```bash
   git fetch
   git checkout feature/my-branch
   git pull
   ```
2. Пересоберите образы и перезапустите сервисы (сделает сборку именно из текущей ветки):
   ```bash
   docker compose up -d --build
   ```

Docker Compose собирает образ из исходников, которые находятся в рабочей копии — поэтому после `git checkout` образ автоматически строится из кода выбранной ветки. Дополнительно ничего настраивать не нужно. Если ранее был запущен другой вариант приложения, при необходимости предварительно выполните `docker compose down`.

## Allure-отчет по e2e-тестам

1. Поднимите сервисы приложения (API и БД), чтобы e2e-тесты могли ходить по HTTP. Проще всего использовать Docker Compose:
   ```bash
   docker compose up -d
   ```
2. Запустите e2e-тесты, чтобы собрать результаты Allure (нужен установленный Gradle):
   ```bash
   cd e2e-test
   gradle test
   ```
   После прогона результаты появятся в `e2e-test/build/allure-results`.
3. Сгенерируйте и откройте отчет при помощи Allure CLI:
   ```bash
   allure serve build/allure-results
   ```
   Либо сохраните статику в `build/allure-report` без автоматического открытия браузера:
   ```bash
   allure generate --clean build/allure-results -o build/allure-report
   ```

Убедитесь, что [Allure Commandline](https://github.com/allure-framework/allure2/releases) установлен и доступен в PATH.

## Альтернативные варианты запуска

### Локальная разработка без полного Docker-образа

Если хотите разрабатывать с hot-reload, оставьте БД в Docker, а фронтенд и backend запускайте локально:

1. **Поднять только БД:**
   ```bash
   docker compose up -d db
   ```
2. **Запустить backend (локально):**
   ```bash
   cd backend
   ./gradlew bootRun
   ```
3. **Запустить frontend (локально):**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Vite поднимет dev-сервер на `http://localhost:5173` и будет проксировать `/api` на `http://localhost:18080`.

### Самостоятельный запуск PostgreSQL

При желании можно обойтись без Docker и использовать собственный сервер БД. В этом случае пропустите шаг 1, настройте доступ к базе и передайте переменные окружения `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` перед запуском backend.

## Назначение Dockerfile'ов

- **Dockerfile** (в корне) — собирает фронтенд, прокладывает статические файлы в Spring Boot JAR и запускает единый контейнер, отдающий и API, и SPA на порту `18080`.
- **backend/Dockerfile** — многоэтапная сборка для отдельного деплоя backend (без фронтенда внутри).
- **frontend/Dockerfile** — сборка production-бандла Vite для отдельного контейнерного деплоя SPA.

Отдельные Dockerfile для backend и frontend пригодятся, если нужно разворачивать их по отдельности. Для локальной разработки всё ещё можно запускать сервисы напрямую из исходников.

## Переменные окружения backend

| Переменная           | Назначение                                            | Значение по умолчанию |
|----------------------|--------------------------------------------------------|------------------------|
| `DB_URL`             | JDBC-строка подключения к PostgreSQL                  | `jdbc:postgresql://localhost:55432/test_report` |
| `DB_USERNAME`        | Пользователь PostgreSQL                               | `report` |
| `DB_PASSWORD`        | Пароль PostgreSQL                                     | `report` |
| `COLUMN_CONFIG_PATH` | Путь к JSON с ширинами колонок                        | `classpath:config/column-config.json` |
| `PORT`               | Порт HTTP-сервера Spring Boot                         | `18080` |

## Frontend

При локальном запуске Vite использует прокси на `http://localhost:18080`. Если backend запущен на другом хосте/порту, укажите `VITE_API_BASE_URL`:
```bash
VITE_API_BASE_URL="http://example.com:18080" npm run dev
```

## Конфигурация колонок

SPA получает ширины колонок вместе с данными из backend (`/api/tests`). Сам JSON лежит в `config/column-config.json`. Чтобы изменить значения:

1. Обновите `config/column-config.json` (общий конфиг).
2. При необходимости обновите `backend/src/main/resources/config/column-config.json` (копия для backend).
3. Перезапустите backend — файл перечитывается при старте. Для проверки актуальных значений используйте `GET /api/config/columns`.

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

- Подготавливайте батч по уникальным `testId`.
- Указывайте только изменившиеся поля.
- Для обновления статуса прогона передавайте `runIndex` (1–5) и `runStatus`.
- `runDate` опционален. Если не указать, дата автоматически проставляется текущей.

### Пример Kotlin-клиента (Spring WebClient)

```kotlin
import org.springframework.web.reactive.function.client.WebClient

data class TestUpsertRequest(
    val items: List<TestItem>,
)

data class TestItem(
    val testId: String,
    val category: String? = null,
    val shortTitle: String? = null,
    val runIndex: Int? = null,
    val runStatus: String? = null,
)

val client = WebClient.builder()
    .baseUrl("http://localhost:18080/api")
    .build()

suspend fun sendResults() {
    val payload = TestUpsertRequest(
        items = listOf(
            TestItem(testId = "TC-001", runIndex = 1, runStatus = "PASSED"),
            TestItem(testId = "TC-002", category = "Payments"),
        ),
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

- Таблица стилизована под Google Sheets: сетка, нумерация строк, буквенные заголовки.
- Фиксированные ширины колонок из конфигурации.
- Inline-редактирование: поля сохраняются по `blur`.
- Выпадающие списки для Run Result N (PASSED/FAILED/NOT RUN).
- Автосохранение и отображение статуса «Saving…».
- Кнопка экспорта «Export to Excel».

## Полезные команды

- Запуск backend-тестов: `cd backend && ./gradlew test`
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

Проект готов к запуску «из коробки» по инструкции выше.
