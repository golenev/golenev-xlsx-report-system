package com.example.e2e.ui.pages

import com.codeborne.selenide.CollectionCondition
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.ElementsCollection
import com.codeborne.selenide.ScrollIntoViewOptions.Block.start
import com.codeborne.selenide.ScrollIntoViewOptions.instant
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.SelenideElement
import com.example.e2e.utils.CENTER
import com.example.e2e.utils.typeOf

class MainPage {

    private val headerTitle: SelenideElement = element("h1")
    private val addRowButton: SelenideElement = `$$`("button.secondary-btn").first()
    private val newRow: SelenideElement = element("tr.new-row")
    private val tableRowSelectorPattern = "tbody tr[data-test-id='tr-data-test-id-%s']"
    private val newRowInputs: ElementsCollection = newRow.`$$`("input.cell-input")
    private val newRowTextAreas: ElementsCollection = newRow.`$$`("textarea.cell-textarea")
    private val generalStatusDropdown: SelenideElement = newRow.`$$`("div.status-dropdown").first()
    private val prioritySelect: SelenideElement = newRow.find("select.cell-input")
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
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1))
        newRowInputs.first().typeOf(testId)
    }

    fun fillCategory(category: String) {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(2))
        newRowInputs[1].typeOf(category)
    }

    fun fillShortTitle(shortTitle: String) {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(3))
        newRowInputs[2].typeOf(shortTitle)
    }

    fun fillIssueLink(issueLink: String) {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(4))
        newRowInputs[3].typeOf(issueLink)
    }

    fun selectGeneralStatus(status: String) {
        generalStatusDropdown.shouldBe(visible)
        generalStatusDropdown.find("summary").click()
        generalStatusDropdown.findAll("button.status-option").findBy(text(status)).click()
    }

    fun selectPriority(priority: String) {
        prioritySelect.shouldBe(visible).selectOption(priority)
    }

    fun fillDetailedScenario(scenario: String) {
        newRowTextAreas.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1))
        newRowTextAreas.first().typeOf(scenario)
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

    fun checkPopupWarning() {
        `$`(".popup-message").shouldHave(exactText("Перед остановкой регресса заполните результаты для всех тест-кейсов."))
        `$`(".popup-title").shouldHave(exactText("Не все статусы заполнены"))
    }

    fun closePopupWarning() {
        `$`(".popup-actions .primary-btn").click()
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
        val row = tableRowByTestId(testId).shouldBe(visible)
        val inputs = row.findAll("input.cell-input")
        inputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1))
        return inputs.first()
    }
}
