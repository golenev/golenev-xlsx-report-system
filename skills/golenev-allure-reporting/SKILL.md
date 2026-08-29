---
name: golenev-allure-reporting
description: Structure Allure test metadata, business steps, reusable framework steps, UI event attachments, REST Assured evidence, and failure diagnostics for golenev-xlsx-report-system tests. Use when writing or reviewing @AllureId, @DisplayName, step blocks, @Step methods, Selenide aliases and because clauses, setup/cleanup reporting, proxy evidence, or Allure report readability.
---

# Allure-шаги и отчёт Golenev

## Уровни отчёта

Строить отчёт сверху вниз:

1. `@DisplayName` класса — область или feature.
2. `@DisplayName` теста — проверяемое поведение и ожидаемый результат.
3. `@AllureId` — уникальная связь самостоятельного теста с TestOps.
4. `step("...")` в тесте — бизнес-последовательность сценария.
5. `@Step` во framework — reusable UI/API-операция.
6. Автоматические UI/API attachments — техническое доказательство и диагностика.

## Метаданные теста

- Писать `@DisplayName` на русском, в форме наблюдаемого поведения.
- Назначать один уникальный `@AllureId` каждому самостоятельному `@Test`.
- Проверять существующие ID перед добавлением нового.
- Не объединять разные варианты параметризацией, если им нужны разные Allure ID.
- Не менять существующий Allure ID при рефакторинге поведения того же тест-кейса.

## Бизнес-шаги в тесте

- Использовать `org.golenev.utils.step`, который возвращает результат `Allure.step`.
- Формулировать шаг глаголом во множественном числе: «Открываем», «Формируем», «Отправляем», «Проверяем», «Удаляем».
- Включать значимый параметр в текст: testId, поле, release name, способ закрытия.
- Делить сценарий по наблюдаемым бизнес-действиям, не по отдельным строкам кода.
- Помещать assertions внутрь шага «Проверяем...», чтобы падение имело понятный контекст.
- Оборачивать setup и cleanup в шаги, если они важны для диагностики.
- Возвращать данные из шага вместо создания скрытой mutable-переменной.

```kotlin
val response = step("Отправляем batch без обязательного поля $field") {
    reportService.sendBatch(request, expectedStatus = 400)
}

step("Проверяем ошибку для поля $field") {
    response.statusCode.shouldBe(400, "response.statusCode не совпало с ожидаемым")
}
```

## Framework `@Step`

- Аннотировать reusable Page/Component Object actions и transport operations.
- Описывать внутри аннотации полный результат метода, а не только механический click.
- Подставлять аргументы через `{parameterName}`.
- Не использовать один `@Step`-метод с `when`, если отчёт должен явно показать разные пользовательские действия.
- Делать отдельные методы `closeEditorByEscape`, `closeEditorByCloseButton`, `closeEditorByBackdropClick`.
- Не скрывать финальный выбор пользователя внутри общей Template-функции теста.

## Человекочитаемые UI-вложения

Использовать существующий `ReadableAllureSelenideListener` через `DriverConfig().setup()`.

- Назначать элементам `.name(alias)`; registry связывает alias с нормализованным locator в текущем потоке.
- Добавлять `.because(reason)` к Selenide condition.
- Ожидать, что listener приложит alias, конечный locator, тип CHECK/ACTION, operation, success condition, because, status и error.
- Не включать стандартные шумные Selenide steps: listener уже сохраняет screenshot/page source и формирует текстовые attachments.
- Очищать `UiElementNameRegistry` при настройке каждого драйвера.

```kotlin
private val modal = `$`("[data-testid='test-case-editor-modal']")
    .name("Модальный редактор тест-кейса.")

modal.shouldBe(
    visible.because("после Add Row должен открыться редактор создания")
)
```

## API-вложения

- Пропускать все Rest Assured requests через `BaseSpecification.prepareForRequest`.
- Сохранять фильтр `AllureRestAssured()` для request/response attachments.
- Валидировать status централизованно и затем добавлять бизнес-проверки в тесте.
- Для UI-запроса, перехваченного proxy, парсить body в DTO и сравнивать внутри отдельного шага.
- Не вставлять длинный JSON непосредственно в имя шага; он должен быть attachment или DTO assertion.

## Диагностичность

- Добавлять конкретное сообщение каждому project `shouldBe`.
- Указывать в сообщении фактическое выражение: `actualCreateRequest.scenario`, `reportItem.priority`, `remainingItems`.
- Проверять промежуточное состояние перед конечным действием, если оно объясняет возможное падение.
- При падении UI использовать screenshot, page source и readable attachment; не диагностировать только по верхнему stack trace.
- При падении API читать request/response attachment и structured error body.

## Template и отчёт

- Называть общую функцию с постфиксом `Template`.
- Передавать вариантное действие лямбдой и отдельное человекочитаемое описание.
- Оставлять Template только для общей части отчёта.
- Размещать уникальную концовку прямо в тестовом методе, чтобы Allure показывал точный сценарий.

## Проверка качества отчёта

- Убедиться, что по одним названиям шагов понятны предусловие, действие и результат.
- Убедиться, что каждый UI locator имеет alias и важная condition имеет `because`.
- Убедиться, что cleanup виден или однозначно диагностируется.
- Убедиться, что нет дублирующихся Allure ID.
- Запустить целевой тест и проверить `build/allure-results` либо сгенерированный report, а не только консоль Gradle.
