package org.golenev.ui.pages

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import org.golenev.restapi.endpoints.ScenarioStepRequest

/**
 * Page Object главной страницы Test Report, который хранит только действия уровня страницы и фасады к компонентам.
 */
class MainPage {

    /** Заголовок страницы, по которому проверяется успешное открытие или обновление Test Report. */
    private val headerTitle: SelenideElement = element("h1")

    /** Элемент body страницы, по которому можно снять фокус с активного поля. */
    private val body: SelenideElement get() = `$`("body")

    /** Открывает главную страницу Test Report и проверяет, что заголовок отображается. */
    fun open() {
        Selenide.open("/")
        checkTitle()
    }

    /** Обновляет текущую страницу браузера и повторно проверяет заголовок Test Report. */
    fun refreshCurrentPage() {
        Selenide.refresh()
        checkTitle()
    }

    /** Проверяет, что на странице отображается ожидаемый заголовок Test Report. */
    fun checkTitle() {
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

    /** Фасадное действие, которое создаёт draft-строку через таблицу тест-кейсов. */
    fun startNewRow() = Application.testCaseTable.startNewRow()

    /** Заполняет поле Test ID в текущей draft-строке. */
    fun fillTestId(testId: String) = Application.testCaseTable.draftRow.fillTestId(testId)

    /** Заполняет поле Category / Feature в текущей draft-строке. */
    fun fillCategory(category: String) = Application.testCaseTable.draftRow.fillCategory(category)

    /** Заполняет поле Short Title в текущей draft-строке. */
    fun fillShortTitle(shortTitle: String) = Application.testCaseTable.draftRow.fillShortTitle(shortTitle)

    /** Заполняет поле YouTrack Issue Link в текущей draft-строке. */
    fun fillIssueLink(issueLink: String) = Application.testCaseTable.draftRow.fillIssueLink(issueLink)

    /** Выбирает General Test Status в текущей draft-строке. */
    fun selectGeneralStatus(status: String) = Application.testCaseTable.draftRow.selectGeneralStatus(status)

    /** Выбирает Priority в текущей draft-строке. */
    fun selectPriority(priority: String) = Application.testCaseTable.draftRow.selectPriority(priority)

    /** Заполняет текстовый detailed scenario в текущей draft-строке. */
    fun fillDetailedScenario(scenario: String) = Application.testCaseTable.draftRow.fillDetailedScenario(scenario)

    /** Заполняет structured detailed scenario шагами и вложениями в текущей draft-строке. */
    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) =
        Application.testCaseTable.draftRow.fillDetailedScenarioSteps(steps)

    /** Заполняет поле Notes в текущей draft-строке. */
    fun fillNotes(notes: String) = Application.testCaseTable.draftRow.fillNotes(notes)

    /** Сохраняет текущую draft-строку через компонент строки. */
    fun saveNewRow() = Application.testCaseTable.draftRow.saveDraft()

    /** Удаляет существующий тест-кейс с указанным Test ID и подтверждает browser confirm. */
    fun deleteTestCase(testId: String) = Application.testCaseTable.row(testId).delete()

    /** Запускает глобальный regression run с указанным release name. */
    fun startRegression(releaseName: String) = Application.regressionWidget.startRegression(releaseName)

    /** Отменяет текущий глобальный regression run через виджет регресса. */
    fun cancelRegression() = Application.regressionWidget.cancelRegression()

    /** Останавливает текущий глобальный regression run через виджет регресса. */
    fun stopRegress() = Application.regressionWidget.stopRegress()

    /** Выбирает regression status в колонке Regress Run для конкретного тест-кейса. */
    fun selectRegressionStatus(testId: String, status: String) = Application.testCaseTable.row(testId).selectRegressionStatus(status)

    /** Закрывает текущий warning popup. */
    fun closePopupWarning() = Application.warningPopup.close()

    /** Обновляет значение Category / Feature у существующего тест-кейса. */
    fun updateCategory(testId: String, newValue: String) = Application.testCaseTable.row(testId).updateCategory(newValue)

    /** Снимает фокус с активного поля кликом по body страницы. */
    fun unFocus() {
        body.click()
    }

}
