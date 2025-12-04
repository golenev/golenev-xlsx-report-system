package com.example.e2e.tests.ui

import com.example.e2e.db.TestReportTable
import com.example.e2e.db.dbReportExec
import com.example.e2e.dto.Priority
import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.getRandomTestId
import com.example.e2e.utils.step
import io.qameta.allure.AllureId
import org.jetbrains.exposed.sql.deleteWhere
import org.junit.jupiter.api.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@DisplayName("UI: Блокировка кнопки Add Row при редактировании")
class AddRowLockingUiTests {

    private val mainPage = MainPage()
    private val randomTestId = "UI-LOCK-${getRandomTestId()}"

    @BeforeEach
    fun setUp() {
        step("Настраиваем драйвер Selenide") {
            DriverConfig.setup()
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
    @AllureId("171")
    @DisplayName("Кнопка Add Row блокируется во время создания и редактирования")
    fun shouldDisableAddRowWhileEditing() {
        val category = "UI lock"
        val updatedCategory = "UI lock updated"
        val shortTitle = "Lock check"
        val issueLink = "https://youtrack.test/issue/LOCK-1"
        val generalStatus = "Готово"
        val priority = Priority.MEDIUM.value
        val detailedScenario = "Проверка блокировки кнопки Add Row при редактировании"

        step("Открываем главную страницу") { mainPage.open() }
        step("Начинаем создание новой строки") { mainPage.startNewRow() }
        step("Кнопка Add Row заблокирована во время добавления новой строки") {
            mainPage.shouldDisableAddRow()
        }

        step("Заполняем поля для новой строки") {
            mainPage.fillTestId(randomTestId)
            mainPage.fillCategory(category)
            mainPage.fillShortTitle(shortTitle)
            mainPage.fillIssueLink(issueLink)
            mainPage.selectGeneralStatus(generalStatus)
            mainPage.selectPriority(priority)
            mainPage.fillDetailedScenario(detailedScenario)
        }

        step("Кнопка Add Row всё ещё заблокирована во время добавления новой строки") {
            mainPage.shouldDisableAddRow()
        }

        step("Сохраняем новую строку") { mainPage.saveNewRow() }
        step("Кнопка Add Row разблокирована после сохранения") { mainPage.shouldEnableAddRow() }
        step("Проверяем, что тест-кейс отображается в таблице") { mainPage.shouldSeeTestCase(randomTestId) }

        step("Начинаем редактировать поле Category в существующей строке") {
            mainPage.focusOnCategory(randomTestId)
            mainPage.updateCategory(randomTestId, updatedCategory)
        }
    }

}
