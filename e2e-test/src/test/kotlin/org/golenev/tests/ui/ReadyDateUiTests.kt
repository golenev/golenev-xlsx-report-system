package org.golenev.tests.ui

import com.codeborne.selenide.Selenide
import io.qameta.allure.AllureId
import org.golenev.commondto.Priority
import org.golenev.db.dbReportExec
import org.golenev.db.tables.testReportTable.TestReportTable
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.mainPage
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("UI: Автоматическое проставление Ready Date при добавлении тест кейса")
class ReadyDateUiTests {

    private val randomTestId = "UI-LOCK-${getRandomTestId()}"

    @BeforeEach
    fun setUp() {
        DriverConfig().setup()
    }

    @AfterEach
    fun cleaDb() {
        Selenide.closeWebDriver()

        dbReportExec {
            TestReportTable.deleteWhere {
                (testId eq randomTestId)
            }
        }
    }

    @Test
    @AllureId("170")
    @DisplayName("Ready Date автоматически проставляется после сохранения тест-кейса")
    fun shouldAutoFillReadyDateAfterSave() {
        val category = "UI ready date"
        val shortTitle = "Ready date auto"
        val issueLink = "https://youtrack.test/issue/READY-1"
        val generalStatus = "Готово"
        val priority = Priority.MEDIUM.value
        val detailedScenario = "Проверка автоматического заполнения Ready Date"

        val today = LocalDate.now().toString()

        step("Открываем главную страницу") { mainPage.open() }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Открываем модальный редактор создания тест-кейса") { mainPage.header.openCreateEditor() }
        step("Проверяем открытие редактора создания") { mainPage.testCaseEditor.checkVisible() }
        step("Проверяем готовность режима создания") { mainPage.testCaseEditor.checkCreateModeReady() }
        step("Проверяем, что дата готовности в модальном редакторе заполнена сегодняшней датой") { mainPage.testCaseEditor.checkEditorReadyDate(today) }
        step("Указываем идентификатор тест-кейса $randomTestId") { mainPage.testCaseEditor.fillTestId(randomTestId) }
        step("Указываем категорию $category") { mainPage.testCaseEditor.fillCategory(category) }
        step("Указываем название $shortTitle") { mainPage.testCaseEditor.fillShortTitle(shortTitle) }
        step("Указываем ссылку на задачу $issueLink") { mainPage.testCaseEditor.fillIssueLink(issueLink) }
        step("Выбираем статус готовности: $generalStatus") { mainPage.testCaseEditor.selectGeneralStatus(generalStatus) }
        step("Выбираем приоритет: $priority") { mainPage.testCaseEditor.selectPriority(priority) }
        step("Указываем сценарий $detailedScenario") { mainPage.testCaseEditor.scenarioEditor.fillDetailedScenario(detailedScenario) }
        step("Сохраняем новый тест-кейс без изменения даты готовности") { mainPage.testCaseEditor.footer.saveNewTestCase() }
        step("Проверяем закрытие редактора") { mainPage.testCaseEditor.checkEditorClosed() }
        step("Проверяем, что тест-кейс появился в таблице") { mainPage.testCaseTable.checkRowVisible(randomTestId) }
        step("Проверяем, что дата готовности всё ещё заполнена сегодняшней датой") { mainPage.testCaseTable.checkReadyDate(randomTestId, today) }
    }

}
