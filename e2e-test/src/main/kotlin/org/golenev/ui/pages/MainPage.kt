package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.ScrollIntoViewOptions.Block.start
import com.codeborne.selenide.ScrollIntoViewOptions.instant
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.utils.CENTER
import org.golenev.utils.typeOf

class MainPage {

    private val headerTitle: SelenideElement = element("h1")
    private val addRowButton: SelenideElement =
        `$`("button[data-role='button'][data-action='add-row']")
    private val newRow: SelenideElement =
        `$`("[data-testid='test-case-row'][data-state='draft']")
    private val tableRowSelectorPattern = "[data-testid='test-case-row'][data-test-case-id='%s']"
    private val newRowTestIdInput: SelenideElement = newRowField("Test ID").find("textarea, input")
    private val newRowCategoryInput: SelenideElement = newRowField("Category / Feature").find("textarea, input")
    private val newRowShortTitleInput: SelenideElement = newRowField("Short Title").find("textarea, input")
    private val newRowIssueLinkInput: SelenideElement = newRowField("YouTrack Issue Link").find("textarea, input")
    private val newRowScenarioTextarea: SelenideElement = newRowField("Detailed Scenario").find("textarea")
    private val newRowNotesTextarea: SelenideElement = newRowField("Notes").find("textarea")
    private val newRowGeneralStatusDropdown: SelenideElement = newRowField("General Test Status").find("[data-testid='status-dropdown']")
    private val newRowPrioritySelect: SelenideElement = newRowField("Priority").find("select[data-testid='priority-select']")
    private val newRowSaveButton: SelenideElement =
        newRow.find("[data-testid='save-test-case-button']")
    private val regressionActions = `$$`("div.regression-actions")
    private val regressionStartButton: SelenideElement = regressionActions
        .findBy(text("Would you run regress"))
    private val regressionReleaseInput: SelenideElement = element("input.release-input")
    private val regressionSaveButton: SelenideElement = element("div.regression-start-form button.success-btn")
    private val regressionCancelButton: SelenideElement = `$`(".regression-actions .secondary-btn")

    fun shouldDisableAddRow() {
        addRowButton
            .scrollIntoView(instant().block(start))
            .shouldBe(disabled.because("кнопка должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    fun shouldEnableAddRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса"))
    }

    fun shouldDisableSaveNewRow() {
        newRowSaveButton.shouldBe(disabled.because("кнопка должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    fun shouldEnableSaveNewRow() {
        newRowSaveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей"))
    }

    fun open() {
        Selenide.open("/")
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

    fun refreshCurrentPage() {
        com.codeborne.selenide.Selenide.refresh()
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

    fun startNewRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса")).click()
        newRow.shouldBe(visible.because("после нажатия добавления должна появиться черновая строка"))
    }

    fun fillTestId(testId: String) {
        newRowTestIdInput.shouldBe(visible.because("поле Test ID должно быть видимым для ввода значения")).typeOf(testId)
    }

    fun fillCategory(category: String) {
        newRowCategoryInput.shouldBe(visible.because("поле Category должно быть видимым для ввода значения")).typeOf(category)
    }

    fun fillShortTitle(shortTitle: String) {
        newRowShortTitleInput.shouldBe(visible.because("поле Short Title должно быть видимым для ввода значения")).typeOf(shortTitle)
    }

    fun fillIssueLink(issueLink: String) {
        newRowIssueLinkInput.shouldBe(visible.because("поле Issue Link должно быть видимым для ввода значения")).typeOf(issueLink)
    }

    fun selectGeneralStatus(status: String) {
        newRowGeneralStatusDropdown.shouldBe(visible.because("выпадающий список статуса должен быть видимым для выбора значения"))
        newRowGeneralStatusDropdown.find("summary").click()
        newRowGeneralStatusDropdown.findAll("button[data-testid='status-option']").findBy(text(status)).click()
    }

    fun selectPriority(priority: String) {
        newRowPrioritySelect.shouldBe(visible.because("выпадающий список приоритета должен быть видимым для выбора значения")).selectOption(priority)
    }

    fun fillDetailedScenario(scenario: String) {
        newRowScenarioTextarea.shouldBe(visible.because("поле сценария должно быть видимым для ввода значения")).typeOf(scenario)
    }

    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) {
        steps.forEachIndexed { index, step ->
            val row = newRowScenarioRows()[index].shouldBe(visible.because("элемент должен быть видимым на странице"))

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

    private fun fillScenarioStepAttachment(row: SelenideElement, attachment: String) {
        row.find("[data-testid='scenario-attachment-add-button']").shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
        row.find("[data-testid='scenario-attachment-content']").shouldBe(visible.because("элемент должен быть видимым на странице"))
        row.find("[data-testid='scenario-attachment-content']").click()
        row.find("[data-testid='scenario-attachment-content']").shouldBe(visible.because("элемент должен быть видимым для ввода значения")).typeOf(attachment)
        row.find("[data-testid='scenario-attachment-edit-button']").shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
        row.find("[data-testid='scenario-attachment-toggle']").shouldBe(visible.because("элемент должен быть видимым на странице")).shouldHave(text("Вложение").because("после добавления вложения должна появиться плашка с текстом Вложение"))
        row.find("[data-testid='scenario-attachment-content']").should(disappear.because("поле вложения должно закрыться после сохранения текста вложения"))
    }

    fun fillNotes(notes: String) {
        newRowNotesTextarea.shouldBe(visible.because("поле Notes должно быть видимым для ввода значения")).typeOf(notes)
    }

    fun saveNewRow() {
        newRowSaveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей")).click()
        newRow.shouldBe(hidden.because("после сохранения черновая строка должна скрыться"))
    }

    fun shouldSeeTestCase(testId: String) {
        tableRowByTestId(testId).scrollIntoView(CENTER).shouldBe(visible.because("элемент должен быть видимым на странице"))
    }

    fun deleteTestCase(testId: String) {
        val row = tableRowByTestId(testId).shouldBe(visible.because("элемент должен быть видимым на странице"))
        row.find("[data-testid='delete-test-case-button']").click()
        Selenide.confirm()
    }

    fun shouldNotSeeTestCase(testId: String) {
        tableRowByTestId(testId).should(disappear.because("элемент должен исчезнуть после выполненного действия"))
    }

    fun shouldHaveTestCasesCount(expectedCount: Int) {
        `$$`("[data-testid='test-case-row'][data-test-case-id]").shouldHave(size(expectedCount).because("количество строк таблицы должно соответствовать ожидаемому значению"))
    }

    fun shouldHaveReadyDateWhenNewRow(expectedDate: String) {
        newRowField("Ready Date").shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    fun shouldHaveReadyDate(testId: String, expectedDate: String) {
        tableRowByTestId(testId).shouldBe(visible.because("элемент должен быть видимым на странице"))
        existingRowField(testId, "Ready Date").shouldHave(text(expectedDate).because("ячейка Ready Date должна содержать ожидаемую дату"))
    }

    fun openRegressionStartForm() {
        regressionStartButton.shouldBe(enabled.because("элемент должен быть доступен перед кликом")).click()
        regressionReleaseInput.shouldBe(visible.because("элемент должен быть видимым на странице"))
    }

    fun fillRegressionName(releaseName: String) {
        regressionReleaseInput.shouldBe(visible.because("элемент должен быть видимым для ввода значения")).typeOf(releaseName)
    }

    fun saveRegressionStart() {
        regressionSaveButton.shouldBe(enabled.because("элемент должен быть доступен перед кликом")).click()
        regressionCancelButton.shouldBe(visible.because("элемент должен быть видимым на странице"))
    }

    fun startRegression(releaseName: String) {
        openRegressionStartForm()
        fillRegressionName(releaseName)
        saveRegressionStart()
    }

    fun cancelRegression() {
        regressionCancelButton.shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
        regressionCancelButton.should(disappear.because("элемент должен исчезнуть после выполненного действия"))
    }

    fun stopRegress() {
        `$`(".regression-actions .danger-btn").shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
    }

    fun selectRegressionStatus(testId: String, status: String) {
        tableRowByTestId(testId).shouldBe(visible.because("элемент должен быть видимым на странице"))
        existingRowField(testId, "Regress Run").find("[data-testid='regress-run-button']").shouldBe(enabled.because("селект статуса регресса должен быть доступен для выбора значения")).selectOption(status)
    }

    fun checkPopupWarning() {
        `$`(".popup-message").shouldHave(exactText("Перед остановкой регресса заполните результаты для всех тест-кейсов.").because("попап должен объяснять, почему нельзя остановить регресс без заполненных статусов"))
        `$`(".popup-title").shouldHave(exactText("Не все статусы заполнены").because("заголовок попапа должен указывать на незаполненные статусы"))
    }

    fun closePopupWarning() {
        `$`(".popup-actions .secondary-btn").click()
        `$`(".popup-card").shouldBe(disappear.because("попап должен закрыться после нажатия кнопки закрытия"))
    }

    fun updateCategory(testId: String, newValue: String) {
        categoryInput(testId).shouldBe(visible.because("поле категории должно быть видимым для изменения значения")).setValue(newValue)
    }

    fun unFocus() {
        `$`("body").click()
    }

    fun focusOnCategory(testId: String) {
        categoryInput(testId).shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
    }

    private fun tableRowByTestId(testId: String): SelenideElement =
        element(tableRowSelectorPattern.format(testId))

    private fun categoryInput(testId: String): SelenideElement {
        return existingRowField(testId, "Category / Feature").find("textarea, input")
    }

    private fun existingTestIdField(testId: String): SelenideElement =
        existingRowField(testId, "Test ID")

    private fun existingShortTitleInput(testId: String): SelenideElement =
        existingRowField(testId, "Short Title")

    private fun existingIssueLinkInput(testId: String): SelenideElement =
        existingRowField(testId, "YouTrack Issue Link")

    private fun existingScenarioTextarea(testId: String): SelenideElement =
        existingRowField(testId, "Detailed Scenario")

    private fun existingNotesTextarea(testId: String): SelenideElement =
        existingRowField(testId, "Notes")

    private fun existingGeneralStatusDropdown(testId: String): SelenideElement =
        existingRowField(testId, "General Test Status")

    private fun existingPrioritySelect(testId: String): SelenideElement =
        existingRowField(testId, "Priority")

    private fun existingRowField(testId: String, columnName: String): SelenideElement =
        tableRowByTestId(testId).`$`("[data-testid='test-case-cell'][data-name='${columnName}']")

    private fun newRowScenarioRows() =
        newRowField("Detailed Scenario").`$$`("[data-testid='scenario-editor-step']")

    private fun newRowField(columnName: String): SelenideElement =
        newRow.`$`("[data-testid='test-case-cell'][data-name='${columnName}']")
}
