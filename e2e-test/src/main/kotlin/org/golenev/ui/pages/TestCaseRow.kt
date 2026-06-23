package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.utils.CENTER
import org.golenev.utils.typeOf

/**
 * Component Object одной строки таблицы тест-кейсов: draft-строки или сохранённой строки.
 */
class TestCaseRow(
    /** Корневой Selenide-элемент строки таблицы, внутри которого ищутся все ячейки и кнопки. */
    val root: SelenideElement,
    /** Признак draft-строки, который нужен для сохранения отличий в проверках черновика и сохранённой строки. */
    private val isDraft: Boolean,
) {
    /** Поле ввода Test ID внутри строки. */
    val testIdInput: SelenideElement get() = cell("Test ID").find("input")
    /** Поле ввода Category / Feature внутри строки. */
    val categoryInput: SelenideElement get() = cell("Category / Feature").find("textarea, input")
    /** Поле ввода Short Title внутри строки. */
    val shortTitleInput: SelenideElement get() = cell("Short Title").find("textarea, input")
    /** Поле ввода YouTrack Issue Link внутри строки. */
    val issueLinkInput: SelenideElement get() = cell("YouTrack Issue Link").find("input")
    /** Ячейка Ready Date внутри строки. */
    val readyDateCell: SelenideElement get() = cell("Ready Date")
    /** Dropdown General Test Status внутри строки. */
    val generalStatusDropdown: SelenideElement get() = cell("General Test Status").find("[data-testid='status-dropdown']")
    /** Select Priority внутри строки. */
    val prioritySelect: SelenideElement get() = cell("Priority").find("select[data-testid='priority-select']")
    /** Ячейка Detailed Scenario, содержащая textarea или structured scenario editor. */
    val detailedScenarioCell: SelenideElement get() = cell("Detailed Scenario")
    /** Textarea простого detailed scenario внутри ячейки Detailed Scenario. */
    val scenarioTextarea: SelenideElement get() = detailedScenarioCell.find("textarea")
    /** Textarea Notes внутри строки. */
    val notesTextarea: SelenideElement get() = cell("Notes").find("textarea")
    /** Ячейка Regress Run для выбора статуса регресса конкретного тест-кейса. */
    val regressRunCell: SelenideElement get() = cell("Regress Run")
    /** Кнопка сохранения draft-строки. */
    val saveButton: SelenideElement get() = root.find("[data-testid='save-test-case-button']")
    /** Кнопка удаления сохранённой строки тест-кейса. */
    val deleteButton: SelenideElement get() = root.find("[data-testid='delete-test-case-button']")

    /** Прокручивает страницу к строке и проверяет, что строка видима. */
    fun shouldBeVisible() {
        root.scrollIntoView(CENTER).shouldBe(visible.because("элемент должен быть видимым на странице"))
    }

    /** Проверяет, что строка исчезла со страницы после действия. */
    fun shouldDisappear() {
        root.should(disappear.because("элемент должен исчезнуть после выполненного действия"))
    }

    /** Проверяет, что кнопка сохранения draft-строки недоступна. */
    fun shouldDisableSave() {
        saveButton.shouldBe(disabled.because("кнопка должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    /** Проверяет, что кнопка сохранения draft-строки доступна. */
    fun shouldEnableSave() {
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

    /** Заполняет structured detailed scenario шагами и вложениями в строке. */
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

    /** Сохраняет draft-строку и проверяет, что черновик скрылся. */
    fun saveDraft() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей")).click()
        root.shouldBe(hidden.because("после сохранения черновая строка должна скрыться"))
    }

    /** Удаляет сохранённую строку и подтверждает browser confirm. */
    fun delete() {
        root.shouldBe(visible.because("элемент должен быть видимым на странице"))
        deleteButton.click()
        Selenide.confirm()
    }

    /** Проверяет, что ячейка Ready Date содержит ожидаемую дату. */
    fun shouldHaveReadyDate(expectedDate: String) {
        if (!isDraft) {
            root.shouldBe(visible.because("элемент должен быть видимым на странице"))
        }
        readyDateCell.shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    /** Выбирает regression status в колонке Regress Run для этой строки. */
    fun selectRegressionStatus(status: String) {
        root.shouldBe(visible.because("элемент должен быть видимым на странице"))
        regressRunCell.find("[data-testid='regress-run-button']")
            .shouldBe(enabled.because("селект статуса регресса должен быть доступен для выбора значения"))
            .selectOption(status)
    }

    /** Заполняет вложение конкретного шага structured scenario и сворачивает редактор вложения. */
    private fun fillScenarioStepAttachment(row: SelenideElement, attachment: String) {
        row.find("[data-testid='scenario-attachment-add-button']").shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
        row.find("[data-testid='scenario-attachment-content']").shouldBe(visible.because("элемент должен быть видимым на странице"))
        row.find("[data-testid='scenario-attachment-content']").click()
        row.find("[data-testid='scenario-attachment-content']").shouldBe(visible.because("элемент должен быть видимым для ввода значения")).typeOf(attachment)
        row.find("[data-testid='scenario-attachment-edit-button']").shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
        row.find("[data-testid='scenario-attachment-toggle']").shouldBe(visible.because("элемент должен быть видимым на странице")).shouldHave(text("Вложение").because("после добавления вложения должна появиться плашка с текстом Вложение"))
        row.find("[data-testid='scenario-attachment-content']").should(disappear.because("поле вложения должно закрыться после сохранения текста вложения"))
    }

    /** Возвращает коллекцию строк-шагов внутри structured scenario editor этой строки. */
    private fun scenarioRows() = detailedScenarioCell.`$$`("[data-testid='scenario-editor-step']")

    /** Находит ячейку этой строки по UI-имени колонки из data-name. */
    private fun cell(columnName: String): SelenideElement =
        root.`$`("[data-testid='test-case-cell'][data-name='${columnName}']")
}
