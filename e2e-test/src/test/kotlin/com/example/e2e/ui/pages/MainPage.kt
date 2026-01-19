package com.example.e2e.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.ScrollIntoViewOptions.Block.start
import com.codeborne.selenide.ScrollIntoViewOptions.instant
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.SelenideElement
import com.codeborne.selenide.CollectionCondition.size
import com.example.e2e.utils.CENTER
import com.example.e2e.utils.typeOf

class MainPage {

    private val headerTitle: SelenideElement = element("h1")
    private val headerBtnsContainers = `$$`("button.secondary-btn")
    private val addRowButton  = headerBtnsContainers.filter(exactText("Add Row")).first()
    private val newRow: SelenideElement = element("tr.new-row")
    private val tableRowSelectorPattern = "tbody tr[data-test-id='tr-data-test-id-%s']"
    private val newRowTestIdInput: SelenideElement =
        `$`(".new-row [data-test-id='Test ID']")
    private val newRowCategoryInput: SelenideElement =
        `$`(".new-row [data-test-id='Category']")
    private val newRowShortTitleInput: SelenideElement =
        `$`(".new-row [data-test-id='Short Title']")
    private val newRowIssueLinkInput: SelenideElement =
        `$`(".new-row [data-test-id='YouTrack Issue Link']")
    private val newRowScenarioTextarea: SelenideElement =
        `$`(".new-row [data-test-id='Detailed Scenario']")
    private val newRowNotesTextarea: SelenideElement =
        `$`(".new-row [data-test-id='Notes']")
    private val newRowGeneralStatusDropdown: SelenideElement =
        `$`(".new-row [data-test-id='General Test Status']")
    private val newRowPrioritySelect: SelenideElement =
        `$`(".new-row [data-test-id='Priority']")
    private val newRowSaveButton: SelenideElement = newRow.find("button.save-btn")
    private val regressionActions = `$$`("div.regression-actions")
    private val regressionStartButton: SelenideElement = regressionActions
        .findBy(text("Would you run regress"))
    private val regressionReleaseInput: SelenideElement = element("input.release-input")
    private val regressionSaveButton: SelenideElement = element("div.regression-start-form button.success-btn")
    private val regressionCancelButton: SelenideElement = `$`(".regression-actions .secondary-btn")

    fun shouldDisableAddRow() {
        addRowButton
            .scrollIntoView(instant().block(start))
            .shouldBe(disabled)
    }

    fun shouldEnableAddRow() {
        addRowButton.shouldBe(enabled)
    }

    fun open() {
        Selenide.open("/")
        headerTitle.shouldHave(text("Test Report"))
    }

    fun refreshCurrentPage() {
        com.codeborne.selenide.Selenide.refresh()
        headerTitle.shouldHave(text("Test Report"))
    }

    fun startNewRow() {
        addRowButton.shouldBe(enabled).click()
        newRow.shouldBe(visible)
    }

    fun fillTestId(testId: String) {
        newRowTestIdInput.shouldBe(visible).typeOf(testId)
    }

    fun fillCategory(category: String) {
        newRowCategoryInput.shouldBe(visible).typeOf(category)
    }

    fun fillShortTitle(shortTitle: String) {
        newRowShortTitleInput.shouldBe(visible).typeOf(shortTitle)
    }

    fun fillIssueLink(issueLink: String) {
        newRowIssueLinkInput.shouldBe(visible).typeOf(issueLink)
    }

    fun selectGeneralStatus(status: String) {
        newRowGeneralStatusDropdown.shouldBe(visible)
        newRowGeneralStatusDropdown.find("summary").click()
        newRowGeneralStatusDropdown.findAll("button.status-option").findBy(text(status)).click()
    }

    fun selectPriority(priority: String) {
        newRowPrioritySelect.shouldBe(visible).selectOption(priority)
    }

    fun fillDetailedScenario(scenario: String) {
        newRowScenarioTextarea.shouldBe(visible).typeOf(scenario)
    }

    fun fillNotes(notes: String) {
        newRowNotesTextarea.shouldBe(visible).typeOf(notes)
    }

    fun saveNewRow() {
        newRowSaveButton.shouldBe(enabled).click()
        newRow.shouldBe(hidden)
    }

    fun shouldSeeTestCase(testId: String) {
        tableRowByTestId(testId).scrollIntoView(CENTER).shouldBe(visible)
    }

    fun deleteTestCase(testId: String) {
        val row = tableRowByTestId(testId).shouldBe(visible)
        row.find("button.delete-btn").click()
    }

    fun shouldNotSeeTestCase(testId: String) {
        tableRowByTestId(testId).should(disappear)
    }

    fun shouldHaveTestCasesCount(expectedCount: Int) {
        `$$`("tbody tr[data-test-id^='tr-data-test-id-']").shouldHave(size(expectedCount))
    }

    fun shouldHaveReadyDateWhenNewRow(expectedDate: String) {
        `$`(".new-row [data-ready-date-value='${expectedDate}']").shouldBe(visible)
    }

    fun shouldHaveReadyDate(testId: String, expectedDate: String) {
        val row = `$`("[data-test-id='tr-data-test-id-${testId}']").shouldBe(visible)
        row.`$`("[data-ready-date-value='${expectedDate}']").shouldBe(visible)
    }

    fun openRegressionStartForm() {
        regressionStartButton.shouldBe(enabled).click()
        regressionReleaseInput.shouldBe(visible)
    }

    fun fillRegressionName(releaseName: String) {
        regressionReleaseInput.shouldBe(visible).typeOf(releaseName)
    }

    fun saveRegressionStart() {
        regressionSaveButton.shouldBe(enabled).click()
        regressionCancelButton.shouldBe(visible)
    }

    fun startRegression(releaseName: String) {
        openRegressionStartForm()
        fillRegressionName(releaseName)
        saveRegressionStart()
    }

    fun cancelRegression() {
        regressionCancelButton.shouldBe(visible).click()
        regressionCancelButton.should(disappear)
    }

    fun stopRegress() {
        `$`(".regression-actions .danger-btn").shouldBe(visible).click()
    }

    fun selectRegressionStatus(testId: String, status: String) {
        val row = tableRowByTestId(testId).shouldBe(visible)
        row.`$`("[data-test-id='Regress Run']").shouldBe(enabled).selectOption(status)
    }

    fun checkPopupWarning() {
        `$`(".popup-message").shouldHave(exactText("Перед остановкой регресса заполните результаты для всех тест-кейсов."))
        `$`(".popup-title").shouldHave(exactText("Не все статусы заполнены"))
    }

    fun closePopupWarning() {
        `$`(".popup-actions .secondary-btn").click()
        `$`(".popup-card").shouldBe(disappear)
    }

    fun updateCategory(testId: String, newValue: String) {
        categoryInput(testId).value = newValue
    }

    fun unFocus() {
        `$`("body").click()
    }

    fun focusOnCategory(testId: String) {
        categoryInput(testId).shouldBe(visible).click()
    }

    private fun tableRowByTestId(testId: String): SelenideElement =
        element(tableRowSelectorPattern.format(testId))

    private fun categoryInput(testId: String): SelenideElement {
        return existingRowField(testId, "Category")
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

    private fun existingRowField(testId: String, columnDataTestId: String): SelenideElement =
        `$`("[data-test-id='tr-data-test-id-${testId}'] [data-test-id='${columnDataTestId}']")
}
