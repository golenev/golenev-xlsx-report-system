package com.example.e2e.tests.ui

import com.example.e2e.dto.Priority
import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.step
import io.qameta.allure.AllureId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UI: Блокировка кнопки Add Row при редактировании")
class AddRowLockingUiTests {

    private val mainPage = MainPage()

    @BeforeAll
    fun setUp() {
        step("Настраиваем драйвер Selenide") {
            DriverConfig.setup()
        }
    }

    @Test
    @AllureId("171")
    @DisplayName("Кнопка Add Row блокируется во время создания и редактирования")
    fun shouldDisableAddRowWhileEditing() {
        val testId = "UI-LOCK-${System.currentTimeMillis()}"
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
            mainPage.fillTestId(testId)
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
        step("Проверяем, что тест-кейс отображается в таблице") { mainPage.shouldSeeTestCase(testId) }

        step("Начинаем редактировать поле Category в существующей строке") {
            mainPage.focusOnCategory(testId)
            mainPage.updateCategory(testId, updatedCategory)
        }

        step("Кнопка Add Row заблокирована во время редактирования существующей строки") {
            mainPage.shouldDisableAddRow()
        }

        step("Убираем фокус из инпута, чтобы выполнился гет запрос в фоне") {
            mainPage.unFocus()
        }

        step("Кнопка Add Row снова доступна после завершения редактирования") {
            mainPage.shouldEnableAddRow()
        }

        step("Удаляем созданный тест-кейс") { mainPage.deleteTestCase(testId) }
        step("Убеждаемся, что тест-кейс удалён") { mainPage.shouldNotSeeTestCase(testId) }
    }
}
