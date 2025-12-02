package com.example.e2e.ui.pages

import com.codeborne.selenide.CollectionCondition
import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.disabled
import com.codeborne.selenide.Condition.enabled
import com.codeborne.selenide.Condition.hidden
import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Condition.value
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.ElementsCollection
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.SelenideElement
import org.junit.jupiter.api.DisplayName

@DisplayName("UI: Главная страница")
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

    fun shouldDisableAddRow() {
        addRowButton.shouldBe(disabled)
    }

    fun shouldEnableAddRow() {
        addRowButton.shouldBe(enabled)
    }

    fun open() {
        open("/")
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
        newRowInputs.first().type(testId)
    }

    fun fillCategory(category: String) {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(2))
        newRowInputs[1].type(category)
    }

    fun fillShortTitle(shortTitle: String) {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(3))
        newRowInputs[2].type(shortTitle)
    }

    fun fillIssueLink(issueLink: String) {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(4))
        newRowInputs[3].type(issueLink)
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
        newRowTextAreas.first().type(scenario)
    }

    fun saveNewRow() {
        newRowSaveButton.shouldBe(enabled).click()
        newRow.shouldBe(hidden)
    }

    fun shouldSeeTestCase(testId: String) {
        tableRowByTestId(testId).shouldBe(visible)
    }

    fun deleteTestCase(testId: String) {
        val row = tableRowByTestId(testId).shouldBe(visible)
        row.find("button.delete-btn").click()
    }

    fun shouldNotSeeTestCase(testId: String) {
        tableRowByTestId(testId).should(disappear)
    }

    fun shouldHaveReadyDate(testId: String, expectedDate: String) {
        val row = tableRowByTestId(testId).shouldBe(visible)
        row.find("input[type='date']").shouldBe(visible).shouldHave(value(expectedDate))
    }

    fun updateCategory(testId: String, newValue: String) {
        categoryInput(testId).shouldBe(enabled).setValue(newValue)
    }

    fun focusOnCategory(testId: String) {
        categoryInput(testId).shouldBe(visible).click()
    }

    fun blurFocusedElement() {
        executeJavaScript<Unit>("document.activeElement && document.activeElement.blur()")
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
