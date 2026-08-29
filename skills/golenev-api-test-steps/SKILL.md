---
name: golenev-api-test-steps
description: Build and maintain REST Assured API framework steps, endpoint DAOs, DTOs, response validation, and API/E2E tests for golenev-xlsx-report-system. Use when adding endpoints, request or response models, positive or negative API scenarios, batch operations, forceUpdate behavior, API setup/cleanup, or API assertions in the e2e-test module.
---

# Построение API-шагов Golenev

## Архитектура

Соблюдать уровни:

```text
API/E2E test
  -> endpoint DAO (например ReportServiceDao)
    -> RequestExecutor
      -> BaseSpecification + ResponseValidator
        -> RestAssured
```

- Хранить URI в `Paths`.
- Хранить transport-вызовы конкретного ресурса в endpoint DAO.
- Хранить общую отправку GET/POST/DELETE в `RequestExecutor`.
- Хранить base URI, JSON mapper, content type и `AllureRestAssured` в `BaseSpecification`.
- Валидировать ожидаемый HTTP status централизованно через `ResponseValidator`.
- Хранить request/response DTO рядом с endpoint-слоем.

## Добавление endpoint-метода

1. Добавить путь в `Paths`, если он новый.
2. Добавить типизированный request/response DTO.
3. Добавить публичный метод endpoint DAO.
4. Сформировать request через `baseRequest()`, передать body/query/path параметры.
5. Передать `expectedStatus` с default для позитивного сценария.
6. Вернуть `Response`, если тесту нужны headers/error body; вернуть типизированный DTO для обычного чтения.

```kotlin
fun sendBatch(request: TestBatchRequest, expectedStatus: Int = 200): Response =
    postRequest(
        url = Paths.REPORTS_BATCH.path,
        requestSpecification = baseRequest().body(request),
        expectedStatus = expectedStatus,
    )
```

Не размещать assertions бизнес-полей внутри DAO.

## Формирование теста

- Давать классу `@DisplayName("API: ...")`, тесту уникальные `@AllureId` и `@DisplayName`.
- Строить сценарий из читаемых фаз: подготовить данные, отправить запрос, получить состояние, проверить контракт, очистить данные.
- Оборачивать каждую фазу в `step`.
- Возвращать значения из `step`, если они нужны дальше.
- Для positive flow использовать типизированные DTO.
- Для intentionally invalid JSON использовать `sendTestBody(Any)` с `Map` или другим некорректным типом.
- Передавать ожидаемый негативный status в DAO, чтобы central validator не ожидал 200.
- После этого отдельно проверять error DTO: `status`, `error`, `message`, `missingField`, `path`.

```kotlin
val response = step("Отправляем batch без обязательного поля $field") {
    reportService.sendBatch(
        request = TestBatchRequest(items = listOf(payload)),
        expectedStatus = 400,
    )
}
val error = response.`as`(ErrorResponse::class.java)

step("Проверяем ошибку обязательного поля $field") {
    response.statusCode.shouldBe(400, "response.statusCode не совпало с ожидаемым")
    error.missingField.shouldBe(field, "error.missingField не совпало с ожидаемым")
}
```

## Данные и assertions

- Использовать `TestDataGenerator` для реалистичных массовых данных.
- Создавать structured scenario через `ScenarioRequest`, `ScenarioStepRequest`, `ScenarioAttachmentRequest`.
- Всегда задавать `attachments = emptyList()` для шага без вложений, если DTO-контракт ожидает массив.
- Сопоставлять batch-ответ по `testId`, а не по позиции списка.
- Использовать `shouldNotBeNull()` перед разыменованием nullable-поля.
- Сравнивать каждое важное поле через project matcher с диагностическим message.
- Проверять не только status code, но и сохранённое/возвращённое состояние.
- Для API+UI сценария проверять request body через proxy либо результат через API/БД.

## Позитивные и негативные сценарии

- Для create/update проверять round trip: отправить DTO, прочитать report, найти `testId`, сравнить поля.
- Для `forceUpdate` явно выбирать `sendTest`, `sendBatch` или `sendForceBatch` согласно контракту.
- Для invariant-полей фиксировать исходное значение и сравнивать его после update.
- Для structured scenario проверять порядок шагов, номера, текст, вложения и фильтрацию пустых шагов.
- Для негативного сценария проверять точный endpoint path и структурированную ошибку.
- Использовать параметризацию для однотипной contract-валидации только когда один Allure ID для набора допустим; иначе создавать отдельные тесты и Template.

## Очистка

- Регистрировать созданный `testId` до отправки запроса, чтобы cleanup сработал и после промежуточного падения.
- Удалять созданные записи в `@AfterEach` через DAO БД или API.
- Очищать список ID после удаления.
- Не удалять чужие записи широким запросом без необходимости.
- Делать testId уникальным через `getRandomTestId()`.

## Rest Assured и Allure

- Не переопределять mapper в тесте: общий mapper уже поддерживает Kotlin и Java Time.
- Не отключать `AllureRestAssured`: request/response должны попадать в отчёт.
- Не дублировать базовый URL в endpoint DAO или тесте; использовать `Environment.BASE_URI`.
- Оставлять request/response logging для диагностики validation failure.
- Не логировать вручную secrets или внешние credentials.
