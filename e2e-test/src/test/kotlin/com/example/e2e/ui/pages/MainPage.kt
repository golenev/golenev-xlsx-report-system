package com.example.e2e.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide
import com.example.e2e.ui.components.Button
import com.example.e2e.ui.components.DropDown
import com.example.e2e.ui.components.Input
import com.example.e2e.ui.components.Row
import com.example.e2e.ui.components.TextArea
import com.example.e2e.ui.core.Container
import com.example.e2e.ui.core.containerShould
import com.example.e2e.ui.core.containerShouldNot
import com.example.e2e.ui.core.rowByTestId
import com.example.e2e.ui.core.withTitle
import com.example.e2e.utils.CENTER
import org.openqa.selenium.By

class MainPage : Container(by = By.tagName("body"), context = null) {

    val headerTitle: Container by lazy { Container(context = this, by = By.cssSelector("h1")).withTitle("Header Title") }

    private val headerButtons
        get() = self.findAll("button.secondary-btn")

    val addRowButton: Button
        get() = Button(headerButtons.findBy(exactText("Add Row"))).withTitle("Add Row")

    val newRow: Row by lazy { Row(context = this, by = By.cssSelector("tr.new-row")).withTitle("New Row") }

    val newRowTestId: Input by lazy {
        Input(context = this, by = By.cssSelector(".new-row [data-test-id='Test ID']")).withTitle("Test ID")
    }
    val newRowCategory: Input by lazy {
        Input(context = this, by = By.cssSelector(".new-row [data-test-id='Category']")).withTitle("Category")
    }
    val newRowShortTitle: Input by lazy {
        Input(context = this, by = By.cssSelector(".new-row [data-test-id='Short Title']")).withTitle("Short Title")
    }
    val newRowIssueLink: Input by lazy {
        Input(context = this, by = By.cssSelector(".new-row [data-test-id='YouTrack Issue Link']"))
            .withTitle("YouTrack Issue Link")
    }
    val newRowScenario: TextArea by lazy {
        TextArea(context = this, by = By.cssSelector(".new-row [data-test-id='Detailed Scenario']"))
            .withTitle("Detailed Scenario")
    }
    val newRowNotes: TextArea by lazy {
        TextArea(context = this, by = By.cssSelector(".new-row [data-test-id='Notes']")).withTitle("Notes")
    }
    val newRowGeneralStatus: DropDown by lazy {
        DropDown(context = this, by = By.cssSelector(".new-row [data-test-id='General Test Status']"))
            .withTitle("General Test Status")
    }
    val newRowPriority: DropDown by lazy {
        DropDown(context = this, by = By.cssSelector(".new-row [data-test-id='Priority']")).withTitle("Priority")
    }
    val newRowSave: Button by lazy {
        Button(context = newRow, by = By.cssSelector("button.save-btn")).withTitle("Save")
    }

    private val regressionStartButton: Button
        get() = Button(self.findAll("div.regression-actions").findBy(text("Would you run regress")))
            .withTitle("Regression Start")

    private val regressionReleaseInput: Input by lazy {
        Input(context = this, by = By.cssSelector("input.release-input")).withTitle("Regression Release")
    }
    private val regressionSaveButton: Button by lazy {
        Button(context = this, by = By.cssSelector("div.regression-start-form button.success-btn")).withTitle("Save")
    }
    private val regressionCancelButton: Button by lazy {
        Button(context = this, by = By.cssSelector(".regression-actions .secondary-btn")).withTitle("Cancel")
    }

    fun shouldDisableAddRow() {
        addRowButton.scrollToIfNotVisible()
        addRowButton containerShould disabled
    }

    fun shouldEnableAddRow() {
        addRowButton containerShould enabled
    }

    fun open() {
        Selenide.open("/")
        headerTitle.containerShould(text("Test Report"))
    }

    fun refreshCurrentPage() {
        Selenide.refresh()
        headerTitle.containerShould(text("Test Report"))
    }

    fun startNewRow() {
        addRowButton containerShould enabled
        addRowButton.click()
        newRow containerShould visible
    }

    fun fillTestId(testId: String) {
        newRowTestId containerShould visible
        newRowTestId.type(testId)
    }

    fun fillCategory(category: String) {
        newRowCategory containerShould visible
        newRowCategory.type(category)
    }

    fun fillShortTitle(shortTitle: String) {
        newRowShortTitle containerShould visible
        newRowShortTitle.type(shortTitle)
    }

    fun fillIssueLink(issueLink: String) {
        newRowIssueLink containerShould visible
        newRowIssueLink.type(issueLink)
    }

    fun selectGeneralStatus(status: String) {
        newRowGeneralStatus containerShould visible
        newRowGeneralStatus.self.find("summary").click()
        newRowGeneralStatus.self.findAll("button.status-option").findBy(text(status)).click()
    }

    fun selectPriority(priority: String) {
        newRowPriority containerShould visible
        newRowPriority.selectOption(priority)
    }

    fun fillDetailedScenario(scenario: String) {
        newRowScenario containerShould visible
        newRowScenario.type(scenario)
    }

    fun fillNotes(notes: String) {
        newRowNotes containerShould visible
        newRowNotes.type(notes)
    }

    fun saveNewRow() {
        newRowSave containerShould enabled
        newRowSave.click()
        newRow containerShould hidden
    }

    fun shouldSeeTestCase(testId: String) {
        row(testId).scrollIntoView(CENTER).shouldBe(visible)
    }

    fun deleteTestCase(testId: String) {
        val row = row(testId).self.shouldBe(visible)
        row.find("button.delete-btn").click()
    }

    fun shouldNotSeeTestCase(testId: String) {
        row(testId).self.should(disappear)
    }

    fun shouldHaveTestCasesCount(expectedCount: Int) {
        Selenide.`$$`("tbody tr[data-test-id^='tr-data-test-id-']").shouldHave(size(expectedCount))
    }

    fun shouldHaveReadyDateWhenNewRow(expectedDate: String) {
        Selenide.`$`(".new-row [data-ready-date-value='${expectedDate}']").shouldBe(visible)
    }

    fun shouldHaveReadyDate(testId: String, expectedDate: String) {
        val row = Selenide.`$`("[data-test-id='tr-data-test-id-${testId}']").shouldBe(visible)
        row.`$`("[data-ready-date-value='${expectedDate}']").shouldBe(visible)
    }

    fun openRegressionStartForm() {
        regressionStartButton containerShould enabled
        regressionStartButton.click()
        regressionReleaseInput containerShould visible
    }

    fun fillRegressionName(releaseName: String) {
        regressionReleaseInput containerShould visible
        regressionReleaseInput.type(releaseName)
    }

    fun saveRegressionStart() {
        regressionSaveButton containerShould enabled
        regressionSaveButton.click()
        regressionCancelButton containerShould visible
    }

    fun startRegression(releaseName: String) {
        openRegressionStartForm()
        fillRegressionName(releaseName)
        saveRegressionStart()
    }

    fun cancelRegression() {
        regressionCancelButton containerShould visible
        regressionCancelButton.click()
        regressionCancelButton containerShouldNot visible
    }

    fun stopRegress() {
        val stopButton = Button(context = this, by = By.cssSelector(".regression-actions .danger-btn")).withTitle("Stop")
        stopButton containerShould visible
        stopButton.click()
    }

    fun selectRegressionStatus(testId: String, status: String) {
        val row = row(testId).self.shouldBe(visible)
        row.`$`("[data-test-id='Regress Run']").shouldBe(enabled).selectOption(status)
    }

    fun checkPopupWarning() {
        Selenide.`$`(".popup-message")
            .shouldHave(exactText("Перед остановкой регресса заполните результаты для всех тест-кейсов."))
        Selenide.`$`(".popup-title").shouldHave(exactText("Не все статусы заполнены"))
    }

    fun closePopupWarning() {
        Selenide.`$`(".popup-actions .secondary-btn").click()
        Selenide.`$`(".popup-card").shouldBe(disappear)
    }

    fun updateCategory(testId: String, newValue: String) {
        categoryInput(testId).type(newValue)
    }

    fun unFocus() {
        Selenide.`$`("body").click()
    }

    fun focusOnCategory(testId: String) {
        categoryInput(testId) containerShould visible
        categoryInput(testId).click()
    }

    fun row(testId: String): Row = rowByTestId[testId]

    fun field(testId: String, column: String): Container {
        return Container(context = row(testId), by = By.cssSelector("[data-test-id='${column}']")).withTitle(column)
    }

    private fun categoryInput(testId: String): Input {
        return Input(context = row(testId), by = By.cssSelector("[data-test-id='Category']")).withTitle("Category")
    }
}
