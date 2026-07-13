package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.ui.allure.name
import org.golenev.utils.CENTER
import org.golenev.utils.shouldBeVisibleForInput
import org.golenev.utils.typeOf

/**
 * Component Object одной строки таблицы тест-кейсов: draft-строки или сохранённой строки.
 */
class TestCaseRow(
    rowSelector: String,
    rowName: String,
) {
    private val rowLocator = "[data-testid='test-report-table'] $rowSelector"

    private val row: SelenideElement =
        `$`(rowLocator).name(rowName)

    private val testIdInput: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Test ID'] input")
            .name("Поле ввода Test ID внутри строки.")

    private val categoryInput: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] textarea, $rowLocator [data-testid='test-case-cell'][data-name='Category / Feature'] input")
            .name("Поле ввода Category / Feature внутри строки.")

    private val shortTitleInput: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Short Title'] textarea, $rowLocator [data-testid='test-case-cell'][data-name='Short Title'] input")
            .name("Поле ввода Short Title внутри строки.")

    private val issueLinkInput: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='YouTrack Issue Link'] input")
            .name("Поле ввода YouTrack Issue Link внутри строки.")

    private val readyDateCell: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Ready Date']")
            .name("Ячейка Ready Date внутри строки.")

    private val generalStatusDropdown: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='General Test Status'] [data-testid='status-dropdown']")
            .name("Dropdown General Test Status внутри строки.")

    private val prioritySelect: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Priority'] select[data-testid='priority-select']")
            .name("Select Priority внутри строки.")

    private val scenarioTextarea: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] textarea")
            .name("Textarea простого detailed scenario внутри ячейки Detailed Scenario.")

    private val notesTextarea: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Notes'] textarea")
            .name("Textarea Notes внутри строки.")

    private val regressRunButton: SelenideElement =
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Regress Run'] [data-testid='regress-run-button']")
            .name("Селект Regress Run в строке тест-кейса.")

    private val saveButton: SelenideElement =
        `$`("$rowLocator [data-testid='save-test-case-button']")
            .name("Кнопка сохранения draft-строки.")

    /** Заполняет поле Test ID в строке. */
    fun fillTestId(testId: String) {
        testIdInput.shouldBeVisibleForInput("Test ID").typeOf(testId)
    }

    /** Заполняет поле Category / Feature в строке. */
    fun fillCategory(category: String) {
        categoryInput.shouldBeVisibleForInput("Category").typeOf(category)
    }

    /** Заполняет поле Short Title в строке. */
    fun fillShortTitle(shortTitle: String) {
        shortTitleInput.shouldBeVisibleForInput("Short Title").typeOf(shortTitle)
    }

    /** Заполняет поле YouTrack Issue Link в строке. */
    fun fillIssueLink(issueLink: String) {
        issueLinkInput.shouldBeVisibleForInput("Issue Link").typeOf(issueLink)
    }

    /** Заполняет поле Notes в строке. */
    fun fillNotes(notes: String) {
        notesTextarea.shouldBeVisibleForInput("Notes").typeOf(notes)
    }

    /** Обновляет значение Category / Feature у существующей строки. */
    fun updateCategory(newValue: String) {
        categoryInput
            .shouldBe(visible.because("поле категории должно быть видимым для изменения значения"))
            .setValue(newValue)
    }

    /** Устанавливает фокус в поле Category / Feature у существующей строки. */
    fun focusOnCategory() {
        categoryInput
            .shouldBe(visible.because("поле Category / Feature должно быть видимым перед кликом"))
            .click()
    }

    /** Прокручивает страницу к строке и проверяет, что строка видима. */
    fun checkVisible() {
        row.scrollIntoView(CENTER).shouldBe(visible.because("строка тест-кейса должна быть видимой на странице после прокрутки"))
    }

    /** Проверяет видимость draft-строки сразу после её создания. */
    fun checkVisibleAfterDraftCreation() {
        row.shouldBe(visible.because("после нажатия добавления должна появиться черновая строка"))
    }

    /** Проверяет, что строка исчезла со страницы после действия. */
    fun checkDisappeared() {
        row.shouldBe(disappear.because("строка тест-кейса должна исчезнуть после выполненного действия"))
    }

    /** Проверяет, что кнопка сохранения draft-строки недоступна. */
    fun checkSaveDisabled() {
        saveButton.shouldBe(disabled.because("кнопка сохранения draft-строки должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    /** Проверяет, что кнопка сохранения draft-строки доступна. */
    fun checkSaveEnabled() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей"))
    }

    /** Выбирает значение General Test Status в строке. */
    fun selectGeneralStatus(status: String) {
        generalStatusDropdown.shouldBe(visible.because("выпадающий список статуса должен быть видимым для выбора значения"))
        generalStatusDropdown.`$`("summary").name("Summary dropdown General Test Status внутри строки.").click()
        generalStatusDropdown.`$$`("button[data-testid='status-option']").findBy(text(status)).name("Опция $status в dropdown General Test Status внутри строки.").click()
    }

    /** Выбирает значение Priority в строке. */
    fun selectPriority(priority: String) {
        prioritySelect.shouldBe(visible.because("выпадающий список приоритета должен быть видимым для выбора значения")).selectOption(priority)
    }

    /** Заполняет простой текст detailed scenario в строке. */
    fun fillDetailedScenario(scenario: String) {
        scenarioTextarea.shouldBe(visible.because("поле сценария должно быть видимым для ввода значения")).typeOf(scenario)
    }

    /** Заполняет structured detailed scenario шагами и первым непустым вложением каждого шага. */
    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) {
        steps.forEach { step ->
            `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='${step.number}']")
                .name("Строка шага ${step.number} detailed scenario.")
                .shouldBe(visible.because("строка шага ${step.number} detailed scenario должна быть видимой на странице"))

            `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='${step.number}'] [data-testid='scenario-step-input']")
                .name("Поле ввода текста шага ${step.number} detailed scenario.")
                .shouldBe(visible.because("поле ввода текста шага ${step.number} detailed scenario должно быть видимым для ввода значения"))
                .typeOf(step.text)

            val attachment = step.attachments.firstOrNull { attachment -> attachment.content.isNotBlank() }

            if (attachment != null) {
                fillScenarioStepAttachment(step.number, attachment.content.trim())
            }
        }
    }

    /** Сохраняет draft-строку и проверяет, что черновик скрылся. */
    fun saveDraft() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей")).click()
        row.shouldBe(hidden.because("после сохранения черновая строка должна скрыться"))
    }

    /** Проверяет, что ячейка Ready Date содержит ожидаемую дату. */
    fun checkReadyDate(expectedDate: String) {
        readyDateCell.shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    /** Выбирает regression status в колонке Regress Run для этой строки. */
    fun selectRegressionStatus(status: String) {
        row.shouldBe(visible.because("строка тест-кейса должна быть видимой перед выбором regression status"))
        regressRunButton
            .shouldBe(enabled.because("селект Regress Run в строке тест-кейса должен быть доступен для выбора regression status"))
            .selectOption(status)
    }

    /** Заполняет вложение конкретного шага structured scenario и сворачивает редактор вложения. */
    private fun fillScenarioStepAttachment(stepNumber: Int, attachment: String) {
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-add-button']")
            .name("Кнопка добавления вложения шага detailed scenario.")
            .shouldBe(visible.because("кнопка добавления вложения шага detailed scenario должна быть видимой перед кликом"))
            .click()
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения шага detailed scenario.")
            .shouldBe(visible.because("поле содержимого вложения шага detailed scenario должно быть видимым на странице"))
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения шага detailed scenario.")
            .click()
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения шага detailed scenario.")
            .shouldBe(visible.because("поле содержимого вложения шага detailed scenario должно быть видимым для ввода значения"))
            .typeOf(attachment)
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-edit-button']")
            .name("Кнопка сохранения вложения шага detailed scenario.")
            .shouldBe(visible.because("кнопка сохранения вложения шага detailed scenario должна быть видимой перед кликом"))
            .click()
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-toggle']")
            .name("Плашка вложения шага detailed scenario.")
            .shouldBe(visible.because("плашка вложения шага detailed scenario должна быть видимой на странице"))
            .shouldHave(text("Вложение").because("после добавления вложения должна появиться плашка с текстом Вложение"))
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Detailed Scenario'] [data-testid='scenario-editor-step'][data-step-number='$stepNumber'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения шага detailed scenario.")
            .should(disappear.because("поле вложения должно закрыться после сохранения текста вложения"))
    }
}
