package com.example.e2e.tests.ui

import com.codeborne.selenide.Condition.disabled
import com.codeborne.selenide.Condition.enabled
import com.codeborne.selenide.Selenide
import com.example.e2e.db.dbReportExec
import com.example.e2e.db.tables.TestReportTable
import com.example.e2e.dto.Priority
import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.core.containerShould
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.getRandomTestId
import com.example.e2e.utils.step
import io.qameta.allure.AllureId
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
                (testId eq randomTestId.toString())
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
            mainPage.addRowButton containerShould disabled
        }

        step("Заполняем поля для новой строки") {
            mainPage.newRowTestId.type(randomTestId)
            mainPage.newRowCategory.type(category)
            mainPage.newRowShortTitle.type(shortTitle)
            mainPage.newRowIssueLink.type(issueLink)
            mainPage.selectGeneralStatus(generalStatus)
            mainPage.selectPriority(priority)
            mainPage.newRowScenario.type(detailedScenario)
        }


        step("Кнопка Add Row всё ещё заблокирована во время добавления новой строки") {
            mainPage.addRowButton containerShould disabled
        }

        step("Сохраняем новую строку") { mainPage.saveNewRow() }


        step("Кнопка Add Row разблокирована после сохранения") { mainPage.addRowButton containerShould enabled }
        step("Проверяем, что тест-кейс отображается в таблице") { mainPage.shouldSeeTestCase(randomTestId) }

        step("Начинаем редактировать поле Category в существующей строке") {
            mainPage.updateCategory(randomTestId, updatedCategory)
        }

        step("Кнопка Add Row  заблокирована во время редактирования колонки Category") {
            mainPage.addRowButton containerShould disabled
        }

        step("Уводим фокус из редактируемой ячейки") {
            mainPage.unFocus()
        }

        step("Кнопка Add Row разблокирована после сохранения") { mainPage.addRowButton containerShould enabled }


    }

}
