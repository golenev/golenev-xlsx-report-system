package org.golenev.ui.pages

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.utils.typeOf

class MainPage {

    private val headerTitle: SelenideElement = element("h1")

    val testCaseTable = TestCaseTable()
    val regressionWidget = RegressionWidget()
    val warningPopup = WarningPopup()

    fun open() {
        Selenide.open("/")
        shouldHaveTitle()
    }

    fun refreshCurrentPage() {
        Selenide.refresh()
        shouldHaveTitle()
    }

    fun shouldHaveTitle() {
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

    fun shouldDisableAddRow() = testCaseTable.shouldDisableAddRow()

    fun shouldEnableAddRow() = testCaseTable.shouldEnableAddRow()

    fun shouldDisableSaveNewRow() = testCaseTable.draftRow.shouldDisableSave()

    fun shouldEnableSaveNewRow() = testCaseTable.draftRow.shouldEnableSave()

    fun startNewRow() = testCaseTable.startNewRow()

    fun fillTestId(testId: String) {
        testCaseTable.draftRow.testIdInput.shouldBeVisibleForInput("Test ID").typeOf(testId)
    }

    fun fillCategory(category: String) {
        testCaseTable.draftRow.categoryInput.shouldBeVisibleForInput("Category").typeOf(category)
    }

    fun fillShortTitle(shortTitle: String) {
        testCaseTable.draftRow.shortTitleInput.shouldBeVisibleForInput("Short Title").typeOf(shortTitle)
    }

    fun fillIssueLink(issueLink: String) {
        testCaseTable.draftRow.issueLinkInput.shouldBeVisibleForInput("Issue Link").typeOf(issueLink)
    }

    fun selectGeneralStatus(status: String) = testCaseTable.draftRow.selectGeneralStatus(status)

    fun selectPriority(priority: String) = testCaseTable.draftRow.selectPriority(priority)

    fun fillDetailedScenario(scenario: String) = testCaseTable.draftRow.fillDetailedScenario(scenario)

    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) =
        testCaseTable.draftRow.fillDetailedScenarioSteps(steps)

    fun fillNotes(notes: String) {
        testCaseTable.draftRow.notesTextarea.shouldBeVisibleForInput("Notes").typeOf(notes)
    }

    fun saveNewRow() = testCaseTable.draftRow.saveDraft()

    fun shouldSeeTestCase(testId: String) = testCaseTable.row(testId).shouldBeVisible()

    fun deleteTestCase(testId: String) = testCaseTable.row(testId).delete()

    fun shouldNotSeeTestCase(testId: String) = testCaseTable.row(testId).shouldDisappear()

    fun shouldHaveTestCasesCount(expectedCount: Int) = testCaseTable.shouldHaveRowsCount(expectedCount)

    fun shouldHaveReadyDateWhenNewRow(expectedDate: String) = testCaseTable.draftRow.shouldHaveReadyDate(expectedDate)

    fun shouldHaveReadyDate(testId: String, expectedDate: String) = testCaseTable.row(testId).shouldHaveReadyDate(expectedDate)

    fun openRegressionStartForm() = regressionWidget.openStartForm()

    fun fillRegressionName(releaseName: String) = regressionWidget.fillReleaseName(releaseName)

    fun saveRegressionStart() = regressionWidget.saveRegressionStart()

    fun startRegression(releaseName: String) = regressionWidget.startRegression(releaseName)

    fun cancelRegression() = regressionWidget.cancelRegression()

    fun stopRegress() = regressionWidget.stopRegress()

    fun selectRegressionStatus(testId: String, status: String) = testCaseTable.row(testId).selectRegressionStatus(status)

    fun checkPopupWarning() = warningPopup.shouldHaveDefaultRegressionWarning()

    fun closePopupWarning() = warningPopup.close()

    fun updateCategory(testId: String, newValue: String) {
        testCaseTable.row(testId).categoryInput
            .shouldBe(com.codeborne.selenide.Condition.visible.because("поле категории должно быть видимым для изменения значения"))
            .setValue(newValue)
    }

    fun unFocus() {
        `$`("body").click()
    }

    fun focusOnCategory(testId: String) {
        testCaseTable.row(testId).categoryInput
            .shouldBe(com.codeborne.selenide.Condition.visible.because("элемент должен быть видимым перед кликом"))
            .click()
    }
}
