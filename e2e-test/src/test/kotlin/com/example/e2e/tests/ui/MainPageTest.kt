package com.example.e2e.tests.ui

import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.config.ProxyConfig
import com.example.e2e.ui.config.ProxyInitializer
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.http.Paths
import com.example.e2e.utils.JsonUtils
import com.example.e2e.utils.step
import com.codeborne.selenide.Selenide.closeWebDriver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.Assertions.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainPageTest {

    private val mainPage = MainPage()
    private var proxyThread: Thread? = null

    @BeforeAll
    fun setUp() {
        step("Настраиваем драйвер Selenide") {
            DriverConfig.setup()
        }
    }

    @BeforeEach
    fun setUpProxy(testInfo: TestInfo) {
        step("Запускаем прокси для теста ${testInfo.displayName}") {
            proxyThread = Thread(ProxyInitializer(testInfo.displayName)).also {
                it.start()
                it.join()
            }
        }
    }

    @AfterEach
    fun tearDown() {
        step("Закрываем драйвер и прокси") {
            closeWebDriver()
            proxyThread = null
        }
    }

    @Test
    @DisplayName("Создание и удаление тест-кейса на главной странице")
    fun shouldCreateAndDeleteTestCase() {
        val testId = "UI-4565"
        val category = "UI smoke"
        val shortTitle = "Smoke title"
        val issueLink = "https://youtrack.test/issue/SMK-1"
        val generalStatus = "Готово"
        val detailedScenario = "Пользователь создаёт и удаляет тест-кейс"

        val requestBody = ProxyConfig.interceptRequestBody(Paths.REPORTS.path) {
            mainPage.open()
            mainPage.startNewRow()
            mainPage.fillTestId(testId)
            mainPage.fillCategory(category)
            mainPage.fillShortTitle(shortTitle)
            mainPage.fillIssueLink(issueLink)
            mainPage.selectGeneralStatus(generalStatus)
            mainPage.fillDetailedScenario(detailedScenario)
            mainPage.saveNewRow()
        }

        val createdTest = JsonUtils.parse(requestBody, TestUpsertItem::class.java)
        assertEquals(testId, createdTest.testId)
        assertEquals(category, createdTest.category)
        assertEquals(shortTitle, createdTest.shortTitle)
        assertEquals(issueLink, createdTest.issueLink)
        assertEquals(generalStatus, createdTest.generalStatus)
        assertEquals(detailedScenario, createdTest.scenario)

        mainPage.shouldSeeTestCase(testId)
        mainPage.deleteTestCase(testId)
        mainPage.shouldNotSeeTestCase(testId)
    }
}