package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.ElementsCollection
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

    private val testIdInput: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Test ID'] input")
            .name("Поле ввода Test ID внутри строки.")

    private val categoryInput: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] textarea, $draftRowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] input")
            .name("Поле ввода Category / Feature внутри строки.")

    private val shortTitleInput: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Short Title'] textarea, $draftRowLocator [data-testid='test-case-cell'][data-name='Short Title'] input")
            .name("Поле ввода Short Title внутри строки.")

    private val issueLinkInput: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='YouTrack Issue Link'] input")
            .name("Поле ввода YouTrack Issue Link внутри строки.")

    private val generalStatusDropdown: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='General Test Status'] [data-testid='status-dropdown']")
            .name("Dropdown General Test Status внутри строки.")

    private val prioritySelect: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Priority'] select[data-testid='priority-select']")
            .name("Select Priority внутри строки.")

    private val detailedScenarioTextarea: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] textarea")
            .name("Textarea простого detailed scenario внутри ячейки Detailed Scenario.")

    private val notesTextarea: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Notes'] textarea")
            .name("Textarea Notes внутри строки.")

    private val draftSaveButton: SelenideElement =
        `$`("$draftRowLocator [data-testid='save-test-case-button']")
            .name("Кнопка сохранения draft-строки.")

    private val draftRow: SelenideElement =
        `$`(draftRowLocator)
            .name("Ленивый Selenide-локатор draft-строки, которая появляется только после нажатия Add Row.")

    private val draftReadyDateCell: SelenideElement =
        `$`("$draftRowLocator [data-testid='test-case-cell'][data-name='Ready Date']")
            .name("Ячейка Ready Date внутри строки.")

    private val savedRows: ElementsCollection =
        `$$`("$tableLocator [data-testid='test-case-row']:not([data-state='draft'])")
            .name("Сохранённые строки тест-кейсов")

    @Step("Возвращаем объект существующей строки по Test ID {testId} через операторный доступ table[testId]")
    operator fun get(testId: String): TestCaseTable = apply { checkRowVisible(testId) }

    @Step("Заполняем поле Test ID в текущей draft-строке: {testId}")
    fun fillTestId(testId: String) {
        testIdInput
            .shouldBeVisibleForInput("Test ID")
            .typeOf(testId)
    }

    @Step("Заполняем поле Category / Feature в текущей draft-строке: {category}")
    fun fillCategory(category: String) {
        categoryInput
            .shouldBeVisibleForInput("Category")
            .typeOf(category)
    }

    @Step("Заполняем поле Short Title в текущей draft-строке: {shortTitle}")
    fun fillShortTitle(shortTitle: String) {
        shortTitleInput
            .shouldBeVisibleForInput("Short Title")
            .typeOf(shortTitle)
    }

    @Step("Заполняем поле YouTrack Issue Link в текущей draft-строке: {issueLink}")
    fun fillIssueLink(issueLink: String) {
        issueLinkInput
            .shouldBeVisibleForInput("Issue Link")
            .typeOf(issueLink)
    }

    @Step("Выбираем General Test Status в текущей draft-строке: {status}")
    fun selectGeneralStatus(status: String) {
        generalStatusDropdown.shouldBe(visible.because("выпадающий список статуса должен быть видимым для выбора значения"))
        generalStatusDropdown.`$`("summary").name("Summary dropdown General Test Status внутри строки.").click()
        generalStatusDropdown.`$$`("button[data-testid='status-option']").findBy(text(status)).name("Опция $status в dropdown General Test Status внутри строки.").click()
    }

    @Step("Выбираем Priority в текущей draft-строке: {priority}")
    fun selectPriority(priority: String) {
        prioritySelect
            .shouldBe(visible.because("выпадающий список приоритета должен быть видимым для выбора значения"))
            .selectOption(priority)
    }

    @Step("Заполняем текстовый detailed scenario в текущей draft-строке")
    fun fillDetailedScenario(scenario: String) {
        detailedScenarioTextarea
            .shouldBe(visible.because("поле сценария должно быть видимым для ввода значения"))
            .typeOf(scenario)
    }

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

    @Step("Заполняем поле Notes в текущей draft-строке")
    fun fillNotes(notes: String) {
        notesTextarea
            .shouldBeVisibleForInput("Notes")
            .typeOf(notes)
    }

    @Step("Сохраняем текущую draft-строку")
    fun saveNewRow() {
        draftSaveButton
            .shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей"))
            .click()
        draftRow.shouldBe(hidden.because("после сохранения черновая строка должна скрыться"))
    }

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

    @Step("Обновляем значение Category / Feature у тест-кейса {testId}: {newValue}")
    fun updateCategory(testId: String, newValue: String) {
        val rowLocator = savedRowLocator(testId)
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] textarea, $rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] input")
            .name("Поле ввода Category / Feature внутри строки.")
            .shouldBe(visible.because("поле категории должно быть видимым для изменения значения"))
            .setValue(newValue)
    }

    @Step("Проверяем, что кнопка Add Row недоступна, пока нельзя начать создание новой строки")
    fun checkAddRowDisabled() {
        addRowButton
            .scrollIntoView(instant().block(start))
            .shouldBe(disabled.because("кнопка Add Row должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    @Step("Проверяем, что кнопка Add Row доступна для начала создания тест-кейса")
    fun checkAddRowEnabled() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса"))
    }

    @Step("Проверяем, что сохранённая строка тест-кейса {testId} отображается")
    fun checkRowVisible(testId: String) {
        `$`(savedRowLocator(testId))
            .name("Строка тест-кейса $testId")
            .scrollIntoView(CENTER)
            .shouldBe(visible.because("строка тест-кейса должна быть видимой на странице после прокрутки"))
    }

    @Step("Проверяем, что строка тест-кейса {testId} исчезла со страницы")
    fun checkRowDisappeared(testId: String) {
        `$`(savedRowLocator(testId))
            .name("Строка тест-кейса $testId")
            .shouldBe(disappear.because("строка тест-кейса должна исчезнуть после выполненного действия"))
    }

    @Step("Проверяем количество сохранённых строк тест-кейсов без учёта draft-строки: {expectedCount}")
    fun checkSavedRowsCount(expectedCount: Int) {
        savedRows.shouldHave(size(expectedCount).because("количество сохранённых строк тест-кейсов должно соответствовать ожидаемому"))
    }

    @Step("Нажимаем Add Row и проверяем, что на странице появилась draft-строка")
    fun startNewRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса")).click()
        checkDraftRowVisibleAfterCreation()
    }

    @Step("Проверяем, что кнопка сохранения draft-строки недоступна")
    fun checkDraftSaveDisabled() {
        draftSaveButton.shouldBe(disabled.because("кнопка сохранения draft-строки должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    @Step("Проверяем, что кнопка сохранения draft-строки доступна")
    fun checkDraftSaveEnabled() {
        draftSaveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей"))
    }

    @Step("Проверяем, что Ready Date текущей draft-строки содержит ожидаемую дату {expectedDate}")
    fun checkDraftReadyDate(expectedDate: String) {
        draftReadyDateCell.shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    @Step("Проверяем, что Ready Date сохранённой строки {testId} содержит ожидаемую дату {expectedDate}")
    fun checkReadyDate(testId: String, expectedDate: String) {
        `$`("${savedRowLocator(testId)} [data-testid='test-case-cell'][data-name='Ready Date']")
            .name("Ячейка Ready Date внутри строки.")
            .shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    @Step("Устанавливаем фокус в поле Category / Feature у строки {testId}")
    fun focusOnCategory(testId: String) {
        val rowLocator = savedRowLocator(testId)
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] textarea, $rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] input")
            .name("Поле ввода Category / Feature внутри строки.")
            .shouldBe(visible.because("поле Category / Feature должно быть видимым перед кликом"))
            .click()
    }

    private fun checkDraftRowVisibleAfterCreation() {
        draftRow.shouldBe(visible.because("после нажатия добавления должна появиться черновая строка"))
    }

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
