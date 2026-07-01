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
        step("Настраиваем драйвер Selenide") {
            DriverConfig().setup()
        }
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

        val today = step("Определяем сегодняшнюю дату") { LocalDate.now().toString() }

        step("Открываем главную страницу") { mainPage.open() }
        step("Начинаем создание новой строки") { mainPage.testCaseTable.startNewRow() }
        step("Проверяем, что Ready Date сразу автоматически заполнена сегодняшней датой") { mainPage.testCaseTable.draftRow.checkReadyDate(today) }
        step("Заполняем поле Test ID значением $randomTestId") { mainPage.testCaseTable.fillTestId(randomTestId) }
        step("Заполняем поле Category / Feature значением $category") { mainPage.testCaseTable.fillCategory(category) }
        step("Заполняем поле Short Title значением $shortTitle") { mainPage.testCaseTable.fillShortTitle(shortTitle) }
        step("Заполняем поле YouTrack Issue Link значением $issueLink") { mainPage.testCaseTable.fillIssueLink(issueLink) }
        step("Выбираем значение General Test Status: $generalStatus") { mainPage.testCaseTable.selectGeneralStatus(generalStatus) }
        step("Выбираем значение Priority: $priority") { mainPage.testCaseTable.selectPriority(priority) }
        step("Заполняем поле Detailed Scenario значением $detailedScenario") { mainPage.testCaseTable.fillDetailedScenario(detailedScenario) }
        step("Сохраняем новую строку без указания Ready Date") { mainPage.testCaseTable.saveNewRow() }
        step("Проверяем, что тест-кейс появился в таблице") { mainPage.testCaseTable.checkRowVisible(randomTestId) }
        step("Проверяем, что Ready Date всё ещё заполнена сегодняшней датой") { mainPage.testCaseTable.row(randomTestId).checkReadyDate(today) }
    }

}
