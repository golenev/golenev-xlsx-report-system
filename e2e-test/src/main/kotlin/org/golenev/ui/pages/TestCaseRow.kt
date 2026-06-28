package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.utils.CENTER
import org.golenev.utils.shouldBeVisibleForInput
import org.golenev.utils.typeOf

/**
 * Component Object одной строки таблицы тест-кейсов: draft-строки или сохранённой строки.
 */
class TestCaseRow(
    /** Корневой Selenide-элемент строки таблицы, внутри которого ищутся все ячейки и кнопки. */
    private val root: SelenideElement,
) {
    /** Поле ввода Test ID внутри строки. */
    private val testIdInput: SelenideElement get() = cell("Test ID").find("input")

    /** Поле ввода Category / Feature внутри строки. */
    private val categoryInput: SelenideElement get() = input("Category / Feature")

    /** Поле ввода Short Title внутри строки. */
    private val shortTitleInput: SelenideElement get() = input("Short Title")

    /** Поле ввода YouTrack Issue Link внутри строки. */
    private val issueLinkInput: SelenideElement get() = cell("YouTrack Issue Link").find("input")

    /** Ячейка Ready Date внутри строки. */
    private val readyDateCell: SelenideElement get() = cell("Ready Date")

    /** Dropdown General Test Status внутри строки. */
    private val generalStatusDropdown: SelenideElement get() = cell("General Test Status").find("[data-testid='status-dropdown']")

    /** Select Priority внутри строки. */
    private val prioritySelect: SelenideElement get() = cell("Priority").find("select[data-testid='priority-select']")

    /** Ячейка Detailed Scenario, содержащая textarea или structured scenario editor. */
    private val detailedScenarioCell: SelenideElement get() = cell("Detailed Scenario")

    /** Textarea простого detailed scenario внутри ячейки Detailed Scenario. */
    private val scenarioTextarea: SelenideElement get() = detailedScenarioCell.find("textarea")

    /** Textarea Notes внутри строки. */
    private val notesTextarea: SelenideElement get() = cell("Notes").find("textarea")

    /** Ячейка Regress Run для выбора статуса регресса конкретного тест-кейса. */
    private val regressRunCell: SelenideElement get() = cell("Regress Run")

    /** Кнопка сохранения draft-строки. */
    private val saveButton: SelenideElement get() = root.find("[data-testid='save-test-case-button']")

    /** Кнопка удаления сохранённой строки тест-кейса. */
    private val deleteButton: SelenideElement get() = root.find("[data-testid='delete-test-case-button']")

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
        root.scrollIntoView(CENTER).shouldBe(visible.because("строка тест-кейса должна быть видимой на странице после прокрутки"))
    }

    /** Проверяет видимость draft-строки сразу после её создания. */
    fun checkVisibleAfterDraftCreation() {
        root.shouldBe(visible.because("после нажатия добавления должна появиться черновая строка"))
    }

    /** Проверяет, что строка исчезла со страницы после действия. */
    fun checkDisappeared() {
        root.shouldBe(disappear.because("строка тест-кейса должна исчезнуть после выполненного действия"))
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
        generalStatusDropdown.find("summary").click()
        generalStatusDropdown.findAll("button[data-testid='status-option']").findBy(text(status)).click()
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
            val row = scenarioStep(step.number).shouldBe(visible.because("строка шага ${step.number} detailed scenario должна быть видимой на странице"))

            row.find("[data-testid='scenario-step-input']").shouldBe(visible.because("поле ввода текста шага ${step.number} detailed scenario должно быть видимым для ввода значения")).typeOf(step.text)

            val attachment = step.attachments.firstOrNull { attachment -> attachment.content.isNotBlank() }

            if (attachment != null) {
                fillScenarioStepAttachment(row, attachment.content.trim())
            }
        }
    }

    /** Сохраняет draft-строку и проверяет, что черновик скрылся. */
    fun saveDraft() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей")).click()
        root.shouldBe(hidden.because("после сохранения черновая строка должна скрыться"))
    }

    /** Удаляет сохранённую строку и подтверждает browser confirm. */
    fun delete() {
        root.shouldBe(visible.because("строка тест-кейса должна быть видимой перед удалением"))
        deleteButton.click()
        Selenide.confirm()
    }

    /** Проверяет, что ячейка Ready Date содержит ожидаемую дату. */
    fun checkReadyDate(expectedDate: String) {
        readyDateCell.shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    /** Выбирает regression status в колонке Regress Run для этой строки. */
    fun selectRegressionStatus(status: String) {
        root.shouldBe(visible.because("строка тест-кейса должна быть видимой перед выбором regression status"))
        regressRunCell.find("[data-testid='regress-run-button']")
            .shouldBe(enabled.because("селект Regress Run в строке тест-кейса должен быть доступен для выбора regression status"))
            .selectOption(status)
    }

    /** Заполняет вложение конкретного шага structured scenario и сворачивает редактор вложения. */
    private fun fillScenarioStepAttachment(row: SelenideElement, attachment: String) {
        row.find("[data-testid='scenario-attachment-add-button']").shouldBe(visible.because("кнопка добавления вложения шага detailed scenario должна быть видимой перед кликом")).click()
        row.find("[data-testid='scenario-attachment-content']").shouldBe(visible.because("поле содержимого вложения шага detailed scenario должно быть видимым на странице"))
        row.find("[data-testid='scenario-attachment-content']").click()
        row.find("[data-testid='scenario-attachment-content']").shouldBe(visible.because("поле содержимого вложения шага detailed scenario должно быть видимым для ввода значения")).typeOf(attachment)
        row.find("[data-testid='scenario-attachment-edit-button']").shouldBe(visible.because("кнопка сохранения вложения шага detailed scenario должна быть видимой перед кликом")).click()
        row.find("[data-testid='scenario-attachment-toggle']").shouldBe(visible.because("плашка вложения шага detailed scenario должна быть видимой на странице")).shouldHave(text("Вложение").because("после добавления вложения должна появиться плашка с текстом Вложение"))
        row.find("[data-testid='scenario-attachment-content']").should(disappear.because("поле вложения должно закрыться после сохранения текста вложения"))
    }

    /** Находит строку-шаг внутри structured scenario editor этой строки по номеру шага из data-step-number. */
    private fun scenarioStep(stepNumber: Int): SelenideElement =
        detailedScenarioCell.find("[data-testid='scenario-editor-step'][data-step-number='$stepNumber']")

    /** Находит textarea или input внутри ячейки этой строки по UI-имени колонки из data-name. */
    private fun input(columnName: String): SelenideElement = cell(columnName).find("textarea, input")

    /** Находит ячейку этой строки по UI-имени колонки из data-name. */
    private fun cell(columnName: String): SelenideElement =
        root.find("[data-testid='test-case-cell'][data-name='$columnName']")
}
