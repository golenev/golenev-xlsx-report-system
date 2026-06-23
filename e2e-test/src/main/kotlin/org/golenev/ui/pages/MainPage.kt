package org.golenev.ui.pages

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.utils.typeOf

/**
 * Page Object главной страницы Test Report, который хранит только действия уровня страницы и фасады к компонентам.
 */
class MainPage {

    /** Заголовок страницы, по которому проверяется успешное открытие или обновление Test Report. */
    private val headerTitle: SelenideElement = element("h1")

    /** Компонент таблицы тест-кейсов на главной странице. */
    val testCaseTable = TestCaseTable()
    /** Компонент глобального виджета управления regression run в шапке страницы. */
    val regressionWidget = RegressionWidget()
    /** Компонент warning popup, который появляется при невозможности выполнить действие. */
    val warningPopup = WarningPopup()

    /** Открывает главную страницу Test Report и проверяет, что заголовок отображается. */
    fun open() {
        Selenide.open("/")
        shouldHaveTitle()
    }

    /** Обновляет текущую страницу браузера и повторно проверяет заголовок Test Report. */
    fun refreshCurrentPage() {
        Selenide.refresh()
        shouldHaveTitle()
    }

    /** Проверяет, что на странице отображается ожидаемый заголовок Test Report. */
    fun shouldHaveTitle() {
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

    /** Фасадная проверка, что кнопка добавления строки временно недоступна. */
    fun shouldDisableAddRow() = testCaseTable.shouldDisableAddRow()

    /** Фасадная проверка, что кнопка добавления строки доступна. */
    fun shouldEnableAddRow() = testCaseTable.shouldEnableAddRow()

    /** Фасадная проверка, что кнопка сохранения draft-строки недоступна. */
    fun shouldDisableSaveNewRow() = testCaseTable.draftRow.shouldDisableSave()

    /** Фасадная проверка, что кнопка сохранения draft-строки доступна. */
    fun shouldEnableSaveNewRow() = testCaseTable.draftRow.shouldEnableSave()

    /** Фасадное действие, которое создаёт draft-строку через таблицу тест-кейсов. */
    fun startNewRow() = testCaseTable.startNewRow()

    /** Заполняет поле Test ID в текущей draft-строке. */
    fun fillTestId(testId: String) {
        testCaseTable.draftRow.testIdInput.shouldBeVisibleForInput("Test ID").typeOf(testId)
    }

    /** Заполняет поле Category / Feature в текущей draft-строке. */
    fun fillCategory(category: String) {
        testCaseTable.draftRow.categoryInput.shouldBeVisibleForInput("Category").typeOf(category)
    }

    /** Заполняет поле Short Title в текущей draft-строке. */
    fun fillShortTitle(shortTitle: String) {
        testCaseTable.draftRow.shortTitleInput.shouldBeVisibleForInput("Short Title").typeOf(shortTitle)
    }

    /** Заполняет поле YouTrack Issue Link в текущей draft-строке. */
    fun fillIssueLink(issueLink: String) {
        testCaseTable.draftRow.issueLinkInput.shouldBeVisibleForInput("Issue Link").typeOf(issueLink)
    }

    /** Выбирает General Test Status в текущей draft-строке. */
    fun selectGeneralStatus(status: String) = testCaseTable.draftRow.selectGeneralStatus(status)

    /** Выбирает Priority в текущей draft-строке. */
    fun selectPriority(priority: String) = testCaseTable.draftRow.selectPriority(priority)

    /** Заполняет текстовый detailed scenario в текущей draft-строке. */
    fun fillDetailedScenario(scenario: String) = testCaseTable.draftRow.fillDetailedScenario(scenario)

    /** Заполняет structured detailed scenario шагами и вложениями в текущей draft-строке. */
    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) =
        testCaseTable.draftRow.fillDetailedScenarioSteps(steps)

    /** Заполняет поле Notes в текущей draft-строке. */
    fun fillNotes(notes: String) {
        testCaseTable.draftRow.notesTextarea.shouldBeVisibleForInput("Notes").typeOf(notes)
    }

    /** Сохраняет текущую draft-строку через компонент строки. */
    fun saveNewRow() = testCaseTable.draftRow.saveDraft()

    /** Проверяет, что существующая строка тест-кейса с указанным Test ID отображается. */
    fun shouldSeeTestCase(testId: String) = testCaseTable.row(testId).shouldBeVisible()

    /** Удаляет существующий тест-кейс с указанным Test ID и подтверждает browser confirm. */
    fun deleteTestCase(testId: String) = testCaseTable.row(testId).delete()

    /** Проверяет, что строка тест-кейса с указанным Test ID исчезла со страницы. */
    fun shouldNotSeeTestCase(testId: String) = testCaseTable.row(testId).shouldDisappear()

    /** Проверяет количество существующих строк тест-кейсов в таблице. */
    fun shouldHaveTestCasesCount(expectedCount: Int) = testCaseTable.shouldHaveRowsCount(expectedCount)

    /** Проверяет значение Ready Date в текущей draft-строке. */
    fun shouldHaveReadyDateWhenNewRow(expectedDate: String) = testCaseTable.draftRow.shouldHaveReadyDate(expectedDate)

    /** Проверяет значение Ready Date в существующей строке по Test ID. */
    fun shouldHaveReadyDate(testId: String, expectedDate: String) = testCaseTable.row(testId).shouldHaveReadyDate(expectedDate)

    /** Открывает форму старта глобального regression run через виджет регресса. */
    fun openRegressionStartForm() = regressionWidget.openStartForm()

    /** Заполняет release name в форме старта глобального regression run. */
    fun fillRegressionName(releaseName: String) = regressionWidget.fillReleaseName(releaseName)

    /** Сохраняет запуск глобального regression run после заполнения формы. */
    fun saveRegressionStart() = regressionWidget.saveRegressionStart()

    /** Запускает глобальный regression run с указанным release name. */
    fun startRegression(releaseName: String) = regressionWidget.startRegression(releaseName)

    /** Отменяет текущий глобальный regression run через виджет регресса. */
    fun cancelRegression() = regressionWidget.cancelRegression()

    /** Останавливает текущий глобальный regression run через виджет регресса. */
    fun stopRegress() = regressionWidget.stopRegress()

    /** Выбирает regression status в колонке Regress Run для конкретного тест-кейса. */
    fun selectRegressionStatus(testId: String, status: String) = testCaseTable.row(testId).selectRegressionStatus(status)

    /** Проверяет стандартный warning popup о незаполненных regression status. */
    fun checkPopupWarning() = warningPopup.shouldHaveDefaultRegressionWarning()

    /** Закрывает текущий warning popup. */
    fun closePopupWarning() = warningPopup.close()

    /** Обновляет значение Category / Feature у существующего тест-кейса. */
    fun updateCategory(testId: String, newValue: String) {
        testCaseTable.row(testId).categoryInput
            .shouldBe(com.codeborne.selenide.Condition.visible.because("поле категории должно быть видимым для изменения значения"))
            .setValue(newValue)
    }

    /** Снимает фокус с активного поля кликом по body страницы. */
    fun unFocus() {
        `$`("body").click()
    }

    /** Устанавливает фокус в поле Category / Feature у существующего тест-кейса. */
    fun focusOnCategory(testId: String) {
        testCaseTable.row(testId).categoryInput
            .shouldBe(com.codeborne.selenide.Condition.visible.because("элемент должен быть видимым перед кликом"))
            .click()
    }
}
