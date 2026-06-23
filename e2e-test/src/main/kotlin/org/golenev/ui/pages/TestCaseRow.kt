package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.utils.CENTER
import org.golenev.utils.typeOf

class TestCaseRow(
    val root: SelenideElement,
    private val isDraft: Boolean,
) {
    val testIdInput: SelenideElement get() = cell("Test ID").find("input")
    val categoryInput: SelenideElement get() = cell("Category / Feature").find("textarea, input")
    val shortTitleInput: SelenideElement get() = cell("Short Title").find("textarea, input")
    val issueLinkInput: SelenideElement get() = cell("YouTrack Issue Link").find("input")
    val readyDateCell: SelenideElement get() = cell("Ready Date")
    val generalStatusDropdown: SelenideElement get() = cell("General Test Status").find("[data-testid='status-dropdown']")
    val prioritySelect: SelenideElement get() = cell("Priority").find("select[data-testid='priority-select']")
    val detailedScenarioCell: SelenideElement get() = cell("Detailed Scenario")
    val scenarioTextarea: SelenideElement get() = detailedScenarioCell.find("textarea")
    val notesTextarea: SelenideElement get() = cell("Notes").find("textarea")
    val regressRunCell: SelenideElement get() = cell("Regress Run")
    val saveButton: SelenideElement get() = root.find("[data-testid='save-test-case-button']")
    val deleteButton: SelenideElement get() = root.find("[data-testid='delete-test-case-button']")

    fun shouldBeVisible() {
        root.scrollIntoView(CENTER).shouldBe(visible.because("элемент должен быть видимым на странице"))
    }

    fun shouldDisappear() {
        root.should(disappear.because("элемент должен исчезнуть после выполненного действия"))
    }

    fun shouldDisableSave() {
        saveButton.shouldBe(disabled.because("кнопка должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    fun shouldEnableSave() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей"))
    }

    fun selectGeneralStatus(status: String) {
        generalStatusDropdown.shouldBe(visible.because("выпадающий список статуса должен быть видимым для выбора значения"))
        generalStatusDropdown.find("summary").click()
        generalStatusDropdown.findAll("button[data-testid='status-option']").findBy(text(status)).click()
    }

    fun selectPriority(priority: String) {
        prioritySelect.shouldBe(visible.because("выпадающий список приоритета должен быть видимым для выбора значения")).selectOption(priority)
    }

    fun fillDetailedScenario(scenario: String) {
        scenarioTextarea.shouldBe(visible.because("поле сценария должно быть видимым для ввода значения")).typeOf(scenario)
    }

    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) {
        steps.forEachIndexed { index, step ->
            val row = scenarioRows()[index].shouldBe(visible.because("элемент должен быть видимым на странице"))

            row.find("[data-testid='scenario-step-input']").shouldBe(visible.because("элемент должен быть видимым для ввода значения")).typeOf(step.text)

            val attachment = step.attachments
                .map { attachment -> attachment.content.trim() }
                .filter { attachment -> attachment.isNotBlank() }
                .joinToString(separator = "\n\n")

            if (attachment.isNotBlank()) {
                fillScenarioStepAttachment(row, attachment)
            }
        }
    }

    fun saveDraft() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей")).click()
        root.shouldBe(hidden.because("после сохранения черновая строка должна скрыться"))
    }

    fun delete() {
        root.shouldBe(visible.because("элемент должен быть видимым на странице"))
        deleteButton.click()
        Selenide.confirm()
    }

    fun shouldHaveReadyDate(expectedDate: String) {
        if (!isDraft) {
            root.shouldBe(visible.because("элемент должен быть видимым на странице"))
        }
        readyDateCell.shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    fun selectRegressionStatus(status: String) {
        root.shouldBe(visible.because("элемент должен быть видимым на странице"))
        regressRunCell.find("[data-testid='regress-run-button']")
            .shouldBe(enabled.because("селект статуса регресса должен быть доступен для выбора значения"))
            .selectOption(status)
    }

    private fun fillScenarioStepAttachment(row: SelenideElement, attachment: String) {
        row.find("[data-testid='scenario-attachment-add-button']").shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
        row.find("[data-testid='scenario-attachment-content']").shouldBe(visible.because("элемент должен быть видимым на странице"))
        row.find("[data-testid='scenario-attachment-content']").click()
        row.find("[data-testid='scenario-attachment-content']").shouldBe(visible.because("элемент должен быть видимым для ввода значения")).typeOf(attachment)
        row.find("[data-testid='scenario-attachment-edit-button']").shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
        row.find("[data-testid='scenario-attachment-toggle']").shouldBe(visible.because("элемент должен быть видимым на странице")).shouldHave(text("Вложение").because("после добавления вложения должна появиться плашка с текстом Вложение"))
        row.find("[data-testid='scenario-attachment-content']").should(disappear.because("поле вложения должно закрыться после сохранения текста вложения"))
    }

    private fun scenarioRows() = detailedScenarioCell.`$$`("[data-testid='scenario-editor-step']")

    private fun cell(columnName: String): SelenideElement =
        root.`$`("[data-testid='test-case-cell'][data-name='${columnName}']")
}
