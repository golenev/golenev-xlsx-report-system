package com.example.e2e.ui.tests

import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.step
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainPageTest {

    private val mainPage = MainPage()

    @BeforeAll
    fun setUp() {
        step("Настраиваем драйвер Selenide") {
            DriverConfig.setup()
        }
    }

    @Test
    @DisplayName("Создание и удаление тест-кейса на главной странице")
    fun shouldCreateAndDeleteTestCase() {
        val testId = "UI-${'$'}{System.currentTimeMillis()}"
        val category = "UI smoke"
        val shortTitle = "Smoke title"
        val issueLink = "https://youtrack.test/issue/SMK-1"
        val readyDate = "2024-01-01"
        val generalStatus = "Готово"
        val detailedScenario = "Пользователь создаёт и удаляет тест-кейс"

        mainPage.open()
        mainPage.startNewRow()
        mainPage.fillTestId(testId)
        mainPage.fillCategory(category)
        mainPage.fillShortTitle(shortTitle)
        mainPage.fillIssueLink(issueLink)
        mainPage.fillReadyDate(readyDate)
        mainPage.selectGeneralStatus(generalStatus)
        mainPage.fillDetailedScenario(detailedScenario)
        mainPage.saveNewRow()
        mainPage.shouldSeeTestCase(testId)
        mainPage.deleteTestCase(testId)
        mainPage.shouldNotSeeTestCase(testId)
    }
}
