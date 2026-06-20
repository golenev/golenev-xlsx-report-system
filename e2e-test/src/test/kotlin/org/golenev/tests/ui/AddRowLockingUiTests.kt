package org.golenev.tests.ui

import com.codeborne.selenide.Selenide
import io.qameta.allure.AllureId
import org.golenev.commondto.Priority
import org.golenev.db.dbReportExec
import org.golenev.db.tables.testReportTable.TestReportTable
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.MainPage
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UI: Блокировка кнопки Add Row при редактировании")
class AddRowLockingUiTests {

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
                (testId eq randomTestId)
            }
        }
        Selenide.closeWebDriver()
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


        step("Кнопка Add Row разблокирована после заполнения обязательных полей") {
            mainPage.shouldEnableAddRow()
        }

        step("Сохраняем новую строку") { mainPage.saveNewRow() }


        step("Кнопка Add Row разблокирована после сохранения") { mainPage.shouldEnableAddRow() }
        step("Проверяем, что тест-кейс отображается в таблице") { mainPage.shouldSeeTestCase(randomTestId) }

        step("Начинаем редактировать поле Category в существующей строке") {
            mainPage.updateCategory(randomTestId, updatedCategory)
        }

        step("Кнопка Add Row  заблокирована во время редактирования колонки Category") {
            mainPage.shouldDisableAddRow()
        }

        step("Уводим фокус из редактируемой ячейки") {
            mainPage.unFocus()
        }

        step("Кнопка Add Row разблокирована после сохранения") { mainPage.shouldEnableAddRow() }


    }

}
