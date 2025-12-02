package com.example.e2e.tests.ui

import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.step
import io.qameta.allure.AllureId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("UI: Автоматическое проставление Ready Date при добалвении тест кейса")
class ReadyDateUiTests {

    private val mainPage = MainPage()

    @Test
    @AllureId("170")
    @DisplayName("Ready Date автоматически проставляется после сохранения тест-кейса")
    fun shouldAutoFillReadyDateAfterSave() {
        val testId = "UI-READY-${System.currentTimeMillis()}"
        val category = "UI ready date"
        val shortTitle = "Ready date auto"
        val issueLink = "https://youtrack.test/issue/READY-1"
        val generalStatus = "Готово"
        val detailedScenario = "Проверка автоматического заполнения Ready Date"

        val today = step("Определяем сегодняшнюю дату") { LocalDate.now().toString() }

        step("Открываем главную страницу") { mainPage.open() }
        step("Начинаем создание новой строки") { mainPage.startNewRow() }
        step("Заполняем поле Test ID значением $testId") { mainPage.fillTestId(testId) }
        step("Заполняем поле Category / Feature значением $category") { mainPage.fillCategory(category) }
        step("Заполняем поле Short Title значением $shortTitle") { mainPage.fillShortTitle(shortTitle) }
        step("Заполняем поле YouTrack Issue Link значением $issueLink") { mainPage.fillIssueLink(issueLink) }
        step("Выбираем значение General Test Status: $generalStatus") { mainPage.selectGeneralStatus(generalStatus) }
        step("Заполняем поле Detailed Scenario значением $detailedScenario") { mainPage.fillDetailedScenario(detailedScenario) }

        step("Сохраняем новую строку без указания Ready Date") { mainPage.saveNewRow() }
        step("Проверяем, что тест-кейс появился в таблице") { mainPage.shouldSeeTestCase(testId) }
        step("Проверяем, что Ready Date заполнена сегодняшней датой") { mainPage.shouldHaveReadyDate(testId, today) }
        step("Удаляем созданный тест-кейс") { mainPage.deleteTestCase(testId) }
        step("Убеждаемся, что тест-кейс удалён") { mainPage.shouldNotSeeTestCase(testId) }
    }

}