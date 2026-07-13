package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.ScrollIntoViewOptions.Block.start
import com.codeborne.selenide.ScrollIntoViewOptions.instant
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.ui.allure.name
import org.golenev.utils.CENTER
import org.golenev.utils.shouldBeVisibleForInput
import org.golenev.utils.typeOf
import io.qameta.allure.Step

/**
 * Component Object таблицы тест-кейсов на главной странице Test Report.
 */
class TestCaseTable {
    private val tableLocator = "[data-testid='test-report-table']"
    private val draftRowLocator = "$tableLocator [data-testid='test-case-row'][data-state='draft']"

    private val addRowButton: SelenideElement =
        `$`("button[data-role='button'][data-action='add-row']")
            .name("Кнопка Add Row, которая открывает draft-строку для создания нового тест-кейса.")

    /** Возвращает объект существующей строки по Test ID через операторный доступ table[testId]. */
    @Step("Возвращаем объект существующей строки по Test ID {testId} через операторный доступ table[testId]")
    operator fun get(testId: String): TestCaseTable = apply { checkRowVisible(testId) }

    /** Заполняет поле Test ID в текущей draft-строке. */
    @Step("Заполняем поле Test ID в текущей draft-строке: {testId}")
    fun fillTestId(testId: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Test ID'] input")
            .name("Поле ввода Test ID внутри строки.")
            .shouldBeVisibleForInput("Test ID")
            .typeOf(testId)
    }

    /** Заполняет поле Category / Feature в текущей draft-строке. */
    @Step("Заполняем поле Category / Feature в текущей draft-строке: {category}")
    fun fillCategory(category: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] textarea, $draftRowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] input")
            .name("Поле ввода Category / Feature внутри строки.")
            .shouldBeVisibleForInput("Category")
            .typeOf(category)
    }

    /** Заполняет поле Short Title в текущей draft-строке. */
    @Step("Заполняем поле Short Title в текущей draft-строке: {shortTitle}")
    fun fillShortTitle(shortTitle: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Short Title'] textarea, $draftRowLocator [data-testid='test-case-cell'][data-name='Short Title'] input")
            .name("Поле ввода Short Title внутри строки.")
            .shouldBeVisibleForInput("Short Title")
            .typeOf(shortTitle)
    }

    /** Заполняет поле YouTrack Issue Link в текущей draft-строке. */
    @Step("Заполняем поле YouTrack Issue Link в текущей draft-строке: {issueLink}")
    fun fillIssueLink(issueLink: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='YouTrack Issue Link'] input")
            .name("Поле ввода YouTrack Issue Link внутри строки.")
            .shouldBeVisibleForInput("Issue Link")
            .typeOf(issueLink)
    }

    /** Выбирает General Test Status в текущей draft-строке. */
    @Step("Выбираем General Test Status в текущей draft-строке: {status}")
    fun selectGeneralStatus(status: String) {
        val dropdown = `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='General Test Status'] [data-testid='status-dropdown']")
            .name("Dropdown General Test Status внутри строки.")
        dropdown.shouldBe(visible.because("выпадающий список статуса должен быть видимым для выбора значения"))
        dropdown.`$`("summary").name("Summary dropdown General Test Status внутри строки.").click()
        dropdown.`$$`("button[data-testid='status-option']").findBy(text(status)).name("Опция $status в dropdown General Test Status внутри строки.").click()
    }

    /** Выбирает Priority в текущей draft-строке. */
    @Step("Выбираем Priority в текущей draft-строке: {priority}")
    fun selectPriority(priority: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Priority'] select[data-testid='priority-select']")
            .name("Select Priority внутри строки.")
            .shouldBe(visible.because("выпадающий список приоритета должен быть видимым для выбора значения"))
            .selectOption(priority)
    }

    /** Заполняет текстовый detailed scenario в текущей draft-строке. */
    @Step("Заполняем текстовый detailed scenario в текущей draft-строке")
    fun fillDetailedScenario(scenario: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] textarea")
            .name("Textarea простого detailed scenario внутри ячейки Detailed Scenario.")
            .shouldBe(visible.because("поле сценария должно быть видимым для ввода значения"))
            .typeOf(scenario)
    }

    /** Заполняет structured detailed scenario шагами и вложениями в текущей draft-строке. */
    @Step("Заполняем structured detailed scenario шагами и вложениями в текущей draft-строке")
    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) {
        steps.forEach { step ->
            `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='${step.number}']")
                .name("Строка шага ${step.number} detailed scenario.")
                .shouldBe(visible.because("строка шага ${step.number} detailed scenario должна быть видимой на странице"))

            `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='${step.number}'] [data-testid='scenario-step-input']")
                .name("Поле ввода текста шага ${step.number} detailed scenario.")
                .shouldBe(visible.because("поле ввода текста шага ${step.number} detailed scenario должно быть видимым для ввода значения"))
                .typeOf(step.text)

            val attachment = step.attachments.firstOrNull { attachment -> attachment.content.isNotBlank() }

            if (attachment != null) {
                fillScenarioStepAttachment(step.number, attachment.content.trim())
            }
        }
    }

    /** Заполняет поле Notes в текущей draft-строке. */
    @Step("Заполняем поле Notes в текущей draft-строке")
    fun fillNotes(notes: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Notes'] textarea")
            .name("Textarea Notes внутри строки.")
            .shouldBeVisibleForInput("Notes")
            .typeOf(notes)
    }

    /** Сохраняет текущую draft-строку. */
    @Step("Сохраняем текущую draft-строку")
    fun saveNewRow() {
        `$`("$draftRowLocator [data-testid='save-test-case-button']")
            .name("Кнопка сохранения draft-строки.")
            .shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей"))
            .click()
        `$`(draftRowLocator)
            .name("Ленивый Selenide-локатор draft-строки, которая появляется только после нажатия Add Row.")
            .shouldBe(hidden.because("после сохранения черновая строка должна скрыться"))
    }

    /** Выбирает regression status в колонке Regress Run для конкретного тест-кейса. */
    @Step("Выбираем regression status {status} в колонке Regress Run для тест-кейса {testId}")
    fun selectRegressionStatus(testId: String, status: String) {
        val rowLocator = savedRowLocator(testId)
        `$`(rowLocator)
            .name("Строка тест-кейса $testId")
            .shouldBe(visible.because("строка тест-кейса должна быть видимой перед выбором regression status"))
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Regress Run'] [data-testid='regress-run-button']")
            .name("Селект Regress Run в строке тест-кейса.")
            .shouldBe(enabled.because("селект Regress Run в строке тест-кейса должен быть доступен для выбора regression status"))
            .selectOption(status)
    }

    /** Обновляет значение Category / Feature у существующего тест-кейса. */
    @Step("Обновляем значение Category / Feature у тест-кейса {testId}: {newValue}")
    fun updateCategory(testId: String, newValue: String) {
        val rowLocator = savedRowLocator(testId)
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] textarea, $rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] input")
            .name("Поле ввода Category / Feature внутри строки.")
            .shouldBe(visible.because("поле категории должно быть видимым для изменения значения"))
            .setValue(newValue)
    }

    /** Проверяет, что кнопка Add Row недоступна, пока нельзя начать создание новой строки. */
    @Step("Проверяем, что кнопка Add Row недоступна, пока нельзя начать создание новой строки")
    fun checkAddRowDisabled() {
        addRowButton
            .scrollIntoView(instant().block(start))
            .shouldBe(disabled.because("кнопка Add Row должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    /** Проверяет, что кнопка Add Row доступна для начала создания тест-кейса. */
    @Step("Проверяем, что кнопка Add Row доступна для начала создания тест-кейса")
    fun checkAddRowEnabled() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса"))
    }

    /** Проверяет, что сохранённая строка тест-кейса с указанным Test ID отображается. */
    @Step("Проверяем, что сохранённая строка тест-кейса {testId} отображается")
    fun checkRowVisible(testId: String) {
        `$`(savedRowLocator(testId))
            .name("Строка тест-кейса $testId")
            .scrollIntoView(CENTER)
            .shouldBe(visible.because("строка тест-кейса должна быть видимой на странице после прокрутки"))
    }

    /** Проверяет, что строка тест-кейса с указанным Test ID исчезла со страницы. */
    @Step("Проверяем, что строка тест-кейса {testId} исчезла со страницы")
    fun checkRowDisappeared(testId: String) {
        `$`(savedRowLocator(testId))
            .name("Строка тест-кейса $testId")
            .shouldBe(disappear.because("строка тест-кейса должна исчезнуть после выполненного действия"))
    }

    /** Проверяет количество сохранённых строк тест-кейсов без учёта draft-строки. */
    @Step("Проверяем количество сохранённых строк тест-кейсов без учёта draft-строки: {expectedCount}")
    fun checkSavedRowsCount(expectedCount: Int) {
        `$$`("$tableLocator [data-testid='test-case-row']:not([data-state='draft'])")
            .name("Сохранённые строки тест-кейсов")
            .shouldHave(size(expectedCount).because("количество сохранённых строк тест-кейсов должно соответствовать ожидаемому"))
    }

    /** Нажимает Add Row и проверяет, что на странице появилась draft-строка. */
    @Step("Нажимаем Add Row и проверяем, что на странице появилась draft-строка")
    fun startNewRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса")).click()
        checkDraftRowVisibleAfterCreation()
    }

    /** Проверяет, что кнопка сохранения draft-строки недоступна. */
    @Step("Проверяем, что кнопка сохранения draft-строки недоступна")
    fun checkDraftSaveDisabled() {
        `$`("$draftRowLocator [data-testid='save-test-case-button']")
            .name("Кнопка сохранения draft-строки.")
            .shouldBe(disabled.because("кнопка сохранения draft-строки должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    /** Проверяет, что кнопка сохранения draft-строки доступна. */
    @Step("Проверяем, что кнопка сохранения draft-строки доступна")
    fun checkDraftSaveEnabled() {
        `$`("$draftRowLocator [data-testid='save-test-case-button']")
            .name("Кнопка сохранения draft-строки.")
            .shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей"))
    }

    /** Проверяет, что Ready Date текущей draft-строки содержит ожидаемую дату. */
    @Step("Проверяем, что Ready Date текущей draft-строки содержит ожидаемую дату {expectedDate}")
    fun checkDraftReadyDate(expectedDate: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Ready Date']")
            .name("Ячейка Ready Date внутри строки.")
            .shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    /** Проверяет, что Ready Date сохранённой строки содержит ожидаемую дату. */
    @Step("Проверяем, что Ready Date сохранённой строки {testId} содержит ожидаемую дату {expectedDate}")
    fun checkReadyDate(testId: String, expectedDate: String) {
        `$`("${savedRowLocator(testId)} [data-testid='test-case-cell'][data-name='Ready Date']")
            .name("Ячейка Ready Date внутри строки.")
            .shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    /** Устанавливает фокус в поле Category / Feature у существующей строки. */
    @Step("Устанавливаем фокус в поле Category / Feature у строки {testId}")
    fun focusOnCategory(testId: String) {
        val rowLocator = savedRowLocator(testId)
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] textarea, $rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] input")
            .name("Поле ввода Category / Feature внутри строки.")
            .shouldBe(visible.because("поле Category / Feature должно быть видимым перед кликом"))
            .click()
    }

    private fun checkDraftRowVisibleAfterCreation() {
        `$`(draftRowLocator)
            .name("Ленивый Selenide-локатор draft-строки, которая появляется только после нажатия Add Row.")
            .shouldBe(visible.because("после нажатия добавления должна появиться черновая строка"))
    }

    /** Заполняет вложение конкретного шага structured scenario и сворачивает редактор вложения. */
    @Step("Заполняем вложение шага {stepNumber} structured scenario и сворачиваем редактор вложения")
    private fun fillScenarioStepAttachment(stepNumber: Int, attachment: String) {
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-add-button']")
            .name("Кнопка добавления вложения шага detailed scenario.")
            .shouldBe(visible.because("кнопка добавления вложения шага detailed scenario должна быть видимой перед кликом"))
            .click()
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения шага detailed scenario.")
            .shouldBe(visible.because("поле содержимого вложения шага detailed scenario должно быть видимым на странице"))
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения шага detailed scenario.")
            .click()
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения шага detailed scenario.")
            .shouldBe(visible.because("поле содержимого вложения шага detailed scenario должно быть видимым для ввода значения"))
            .typeOf(attachment)
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-edit-button']")
            .name("Кнопка сохранения вложения шага detailed scenario.")
            .shouldBe(visible.because("кнопка сохранения вложения шага detailed scenario должна быть видимой перед кликом"))
            .click()
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-toggle']")
            .name("Плашка вложения шага detailed scenario.")
            .shouldBe(visible.because("плашка вложения шага detailed scenario должна быть видимой на странице"))
            .shouldHave(text("Вложение").because("после добавления вложения должна появиться плашка с текстом Вложение"))
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения шага detailed scenario.")
            .should(disappear.because("поле вложения должно закрыться после сохранения текста вложения"))
    }

    private fun savedRowLocator(testId: String): String =
        "$tableLocator [data-testid='test-case-row'][data-test-case-id='$testId']"
}
