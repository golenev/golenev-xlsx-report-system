---
name: golenev-test-code-style
description: Enforce the Kotlin/JUnit conventions of the golenev-xlsx-report-system e2e-test module and the verification workflow for repository tests. Use when creating, changing, reviewing, or refactoring tests. E2E-specific Allure and framework rules apply only to e2e-test; backend unit and contract tests use traditional Spring and JUnit style.
---

# Код-стиль тестового фреймворка Golenev

## Область применения

- Правила этого skill про `AllureId`, `step {}`, Selenide lifecycle, endpoint DAO, Page Object, шаблоны сценариев и пакеты `org.golenev.*` относятся только к модулю `e2e-test`.
- Для unit-тестов в `backend/src/test` не применять e2e-код-стиль, Allure-аннотации, бизнес-шаги и слои тестового UI/API-фреймворка.
- Backend unit-тесты писать в традиционном стиле JUnit 5 и Mockito: изолировать тестируемый класс, не поднимать Spring context без необходимости, использовать понятные backtick-имена и структуру arrange/act/assert.
- Backend contract-тесты размещать в отдельном пакете `com.example.report.contract`, поднимать реальный Spring context через `@SpringBootTest` и проверять API через `MockMvc` с тестовой БД.
- В contract-тестах фиксировать HTTP-статус, заголовки, JSON-тело и существенное сохранённое состояние. Использовать детерминированный `Clock` и очищать БД между тестами.
- Для backend-тестов допустимы обычные JUnit assertions и Hamcrest/MockMvc assertions; требование проектного `shouldBe` к ним не относится.

## Рабочий процесс

1. Изучить соседний тест и соответствующий framework-класс до изменения кода.
2. Сохранить разделение ответственности: сценарий — в тесте, взаимодействие — во framework-слое, данные — в DTO/генераторе.
3. В `e2e-test` следовать пакетам `org.golenev.tests`, `org.golenev.ui`, `org.golenev.restapi`, `org.golenev.db`, `org.golenev.utils`; в backend-тестах следовать пакетам production-кода и выделять контрактные тесты в `com.example.report.contract`.
4. Проверить компиляцию целевого модуля; запускать только затронутые тесты, если окружение доступно.
5. Не добавлять generated-файлы Gradle, Allure или локальные артефакты в git.

## Kotlin и структура файлов

- Использовать 4 пробела, Kotlin trailing comma в многострочных аргументах и один пустой раздел между логическими блоками.
- Сохранять порядок: package, imports, KDoc, annotations, class, fields, lifecycle, tests, templates/helpers, companion object.
- Давать классам и функциям имена по поведению, а не по реализации.
- Называть тесты в стиле `should...`, например `shouldWarnWhenClosingDirtyCreateModalByEscape`.
- Называть reusable-объекты по роли: `ReportServiceDao`, `TestCaseTable`, `RegressionWidget`, `TestDataGenerator`.
- Использовать типизированные DTO вместо `Map` и `Any`; применять нетипизированное тело только для намеренно невалидного API-контракта.
- Использовать `val` по умолчанию. Применять `var` и `lateinit` только для изменяемого состояния сценария.
- Выносить повторяющиеся константы теста в private-поля класса; генерировать уникальные идентификаторы для записей, попадающих в БД.
- Использовать проектный `shouldBe(expected, message)` и Kotest matchers; всегда добавлять диагностическое сообщение к сравнению полей.

## KDoc

- Добавлять KDoc над framework-классами, публичными framework-функциями, моделями, extension-функциями и нетривиальными private-helper.
- Описывать назначение и контракт, а не пересказывать строку кода.
- Указывать `@param`, `@return` и свойства модели, когда они не очевидны.
- Объяснять ограничения и причины специальных решений: кэш, потокобезопасность, fallback-селектор, перехват proxy, очистка данных.
- Не заменять KDoc коротким `//` над публичным API. Обычный комментарий оставлять только для локального решения внутри алгоритма.

## Тестовые классы

- Давать классу русскоязычный `@DisplayName`, описывающий область поведения.
- Делать каждый самостоятельный бизнес-сценарий отдельным `@Test` с уникальным `@AllureId`.
- Не использовать параметризацию, если каждому варианту нужен отдельный Allure ID или отдельная бизнес-концовка.
- Для похожих тестов выносить только общий пролог в private-функцию с постфиксом `Template`.
- Передавать различающееся действие в `Template` лямбдой, если тест должен явно показывать конкретное действие.
- Не помещать выбор поведения через `when` внутрь шаблона, если варианты должны быть видны в тестовых методах.
- Оставлять финальные бизнес-действия и проверки явно в каждом тесте.

```kotlin
@Test
@AllureId("310")
@DisplayName("Редактирование продолжается после предупреждения, вызванного клавишей Esc")
fun shouldContinueEditingAfterWarningByEscape() {
    checkClosingWithWarningTemplate("клавишей Esc") {
        mainPage.testCaseTable.closeEditorByEscape()
    }

    step("Нажимаем Продолжить редактирование") {
        mainPage.testCaseTable.continueEditing()
    }
}
```

## Lifecycle и данные

- Настраивать Selenide в `@BeforeEach` через `DriverConfig().setup()`.
- Закрывать WebDriver в `@AfterEach` через `Selenide.closeWebDriver()`.
- Удалять только данные, созданные текущим тестом: хранить `testId`, release name или список созданных ID.
- Делать очистку идемпотентной и выполнять её даже после падения теста.
- Не очищать широкие диапазоны данных, если достаточно удаления по уникальному ID.
- Для E2E-сценария разрешать подготовку через API/БД, действие через UI и независимую проверку через API/БД.

## Границы ответственности

- Не размещать CSS/XPath-селекторы в тестовых классах.
- Не размещать бизнес-assertions внутри transport DAO.
- Не строить REST Assured request напрямую в тесте, если endpoint уже представлен DAO.
- Не скрывать весь тест в одном framework-методе: отчёт должен показывать бизнес-последовательность.
- Не дублировать один и тот же сценарий циклом, если варианты требуют самостоятельной идентификации и отчётности.

## Проверка перед завершением

- Для `e2e-test` проверить отсутствие дублирующихся `AllureId`, имена `Template`-функций, явность вариантных действий и KDoc нового framework API.
- Для backend unit-тестов проверить изоляцию от Spring context и отсутствие e2e/Allure-конструкций.
- Для backend contract-тестов проверить отдельный пакет, реальный Spring context, очистку состояния и полноту HTTP-assertions.
- Выполнить `git diff --check`.
- Скомпилировать затронутый модуль; при доступном окружении запустить только релевантные наборы тестов.

## Действия после коммита

- После каждого созданного коммита сообщить пользователю его hash и предложить самостоятельно запустить unit-тесты backend:
  `.\backend\gradlew.bat -p backend unitTest`.
- В том же сообщении отдельно предложить запустить contract-тесты backend:
  `.\backend\gradlew.bat -p backend contractTest`.
- Предлагать оба запуска даже тогда, когда агент уже выполнил их перед коммитом; явно указывать, какие проверки агент действительно запускал и с каким результатом.
