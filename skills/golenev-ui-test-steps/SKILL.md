---
name: golenev-ui-test-steps
description: Build and maintain Selenide UI framework steps and UI/E2E tests for golenev-xlsx-report-system. Use when adding page objects, component objects, selectors, modal workflows, proxy interception, browser lifecycle, UI assertions, or UI test scenarios under e2e-test/src/main/kotlin/org/golenev/ui and e2e-test/src/test/kotlin/org/golenev/tests.
---

# Построение UI-шагов Golenev

## Архитектура

Сохранять цепочку ответственности:

```text
UI/E2E test
  -> mainPage
    -> MainPage
      -> TestCaseTable / RegressionWidget / WarningPopup
        -> private SelenideElement / ElementsCollection
```

- Хранить бизнес-сценарий и его `step {}` в тесте.
- Хранить локаторы, ожидания, клики и ввод в Page/Component Object.
- Открывать дочерние компоненты через `MainPage`; не создавать их вручную в тестах.
- Добавлять новый Component Object, если блок имеет собственное состояние и несколько действий.

## Локаторы

- Предпочитать стабильные атрибуты: `data-testid`, `data-role`, `data-action`, `data-name`, `data-test-case-id`, `data-scenario-path`.
- Восстанавливать `data-testid` во frontend, если важный интерактивный элемент потерял стабильный контракт тестирования.
- Использовать CSS-класс только для устойчивого структурного элемента, не для случайной визуальной обёртки.
- Использовать XPath по label как совместимый fallback, когда запущенная версия ещё не содержит нового `data-testid`.
- Ограничивать локатор корнем компонента или модалки, чтобы не выбрать одноимённый элемент страницы.
- Давать каждому reusable-локатору `.name("Человекочитаемое имя")` для Allure-вложений.

```kotlin
private val saveButton = `$`("$editorLocator [data-testid='save-test-case-button']")
    .name("Кнопка сохранения модального редактора.")
```

## Framework-шаг

- Аннотировать публичное пользовательское действие `@Step`.
- Делать один метод одним законченным UI-действием или проверкой.
- Перед действием ожидать нужное состояние через `shouldBe`/`shouldHave`.
- После действия проверять наблюдаемый результат: появление, исчезновение, значение или доступность.
- Добавлять `.because("...")` к каждому важному условию.
- Использовать `typeOf()` для controlled React input/textarea: посимвольный ввод корректно обновляет React state; не полагаться на быстрый `setValue`, если запрос сохраняет старое значение.
- Разделять варианты действий отдельными методами вместо enum + `when`.

```kotlin
@Step("Закрываем модальный редактор клавишей Esc")
fun closeEditorByEscape() {
    actions().sendKeys(Keys.ESCAPE).perform()
}

@Step("Закрываем модальный редактор крестиком")
fun closeEditorByCloseButton() {
    closeButton.shouldBe(visible).click()
}
```

## Тестовый сценарий

- Настраивать драйвер в `@BeforeEach`, закрывать его в `@AfterEach`.
- Открывать страницу через `mainPage.open()`, который сам проверяет готовность заголовка.
- Описывать бизнес-шаги русским текстом через `step("...")`.
- Вызывать внутри шага методы Component Object; не работать с локатором напрямую.
- Проверять промежуточные состояния, если они являются частью требования: dirty-status до/после ввода, открытая модалка, видимое предупреждение.
- Проверять конечный эффект независимым способом, когда это важно: UI-строкой, перехваченным request body, API или БД.

## Шаблоны похожих UI-тестов

- Создавать отдельные `@Test`, если вариантам нужны уникальные Allure ID.
- Называть общую private-функцию с постфиксом `Template`.
- Передавать точечное действие лямбдой.
- Заканчивать Template на общей точке сценария.
- Оставлять различающуюся концовку явно внутри теста.

```kotlin
private fun checkClosingWithWarningTemplate(
    actionDescription: String,
    closeAction: () -> Unit,
) {
    step("Пытаемся закрыть модальное окно $actionDescription") { closeAction() }
    step("Проверяем предупреждение") {
        mainPage.testCaseTable.checkUnsavedChangesWarning()
    }
}
```

Не помещать в Template `when(action)` и не скрывать выбор «Не сохранять»/«Продолжить редактирование».

## Модальные окна

- Считать таблицу read-only вне модального редактора, кроме разрешённого `Regress Run` во время регресса.
- Проверять создание и изменение через одну модальную модель, но учитывать disabled Test ID в режиме edit.
- Для dirty-модалки проверять индикатор, предупреждение и обе пользовательские развилки.
- Для клика по backdrop выбирать реально видимую область за пределами модалки, а не центр перекрытого элемента.
- После сохранения или отказа проверять исчезновение модалки.

## Proxy-сценарии

- Получать proxy через `getSelenideProxy()` только после настройки драйвера и открытия браузера.
- Использовать `interceptRequestBody` для проверки сформированного UI-запроса.
- Использовать `interceptResponseBody` для получения ответа и `replaceResponseBody` для контролируемой подмены.
- Ограничивать фильтр endpoint и HTTP-методом; запускать действие внутри переданной лямбды.
- Парсить JSON через `JsonUtils`, затем сравнивать типизированные DTO.

## Антипаттерны

- Не использовать `Thread.sleep`; полагаться на Selenide conditions.
- Не кликать без проверки видимости/доступности, если состояние асинхронно.
- Не дублировать заголовок и редактируемое поле одного значения.
- Не растягивать родительский toggle ради ширины вложенного textarea.
- Не проверять устаревшую inline-логику после переноса редактирования в модалку.
- Не менять read-only просмотр детального сценария при изменении editor workflow.
