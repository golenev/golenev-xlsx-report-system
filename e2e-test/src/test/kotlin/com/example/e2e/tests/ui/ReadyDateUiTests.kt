package com.example.e2e.tests.ui

import com.example.e2e.db.tables.TestReportTable
import com.example.e2e.db.dbReportExec
import com.example.e2e.dto.Priority
import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.getRandomTestId
import com.example.e2e.utils.step
import io.qameta.allure.AllureId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.junit.jupiter.api.*
import java.time.LocalDate

@DisplayName("UI: Автоматическое проставление Ready Date при добавлении тест кейса")
class ReadyDateUiTests {

    private val mainPage = MainPage()
    private val randomTestId = "UI-LOCK-${getRandomTestId()}"

    @BeforeEach
    fun setUp() {
        step("Настраиваем драйвер Selenide") {
            DriverConfig().setup()
        }
    }

    @AfterEach
    fun cleaDb() {
        dbReportExec {
            TestReportTable.deleteWhere {
                (testId eq randomTestId.toString())
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
        step("Начинаем создание новой строки") { mainPage.startNewRow() }
        step("Заполняем поле Test ID значением $randomTestId") { mainPage.fillTestId(randomTestId) }
        step("Заполняем поле Category / Feature значением $category") { mainPage.fillCategory(category) }
        step("Заполняем поле Short Title значением $shortTitle") { mainPage.fillShortTitle(shortTitle) }
        step("Заполняем поле YouTrack Issue Link значением $issueLink") { mainPage.fillIssueLink(issueLink) }
        step("Выбираем значение General Test Status: $generalStatus") { mainPage.selectGeneralStatus(generalStatus) }
        step("Выбираем значение Priority: $priority") { mainPage.selectPriority(priority) }
        step("Заполняем поле Detailed Scenario значением $detailedScenario") { mainPage.fillDetailedScenario(detailedScenario) }

        step("Сохраняем новую строку без указания Ready Date") { mainPage.saveNewRow() }
        step("Проверяем, что тест-кейс появился в таблице") { mainPage.shouldSeeTestCase(randomTestId) }
        step("Проверяем, что Ready Date заполнена сегодняшней датой") { mainPage.shouldHaveReadyDate(randomTestId, today) }

    }

}
