package org.golenev.tests.ui.withProxy

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner.getSelenideProxy
import com.codeborne.selenide.proxy.SelenideProxyServer
import io.qameta.allure.AllureId
import org.golenev.commondto.Priority
import org.golenev.restapi.config.Paths
import org.golenev.restapi.endpoints.*
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.config.interceptResponseBody
import org.golenev.ui.config.replaceResponseBody
import org.golenev.ui.pages.mainPage
import org.golenev.utils.JsonUtils
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DisplayingRowWhenProxyReplacedResponseTest {

    private lateinit var selenideProxy: SelenideProxyServer

    @BeforeEach
    fun setUpProxy() {
        DriverConfig().setup()
        mainPage.open()
        selenideProxy = getSelenideProxy()
    }

    @AfterEach
    fun tearDown() {
        step("Завершаем работу с приложением") {
            Selenide.closeWebDriver()
        }
    }

    @Test
    @AllureId("166")
    @DisplayName("Отображение тест-кейса из подменённого ответа")
    fun shouldDisplayTestCaseFromReplacedResponse() {
        val injectedTestId = "UI-9999"
        val injectedCategory = "Proxy smoke"
        val injectedShortTitle = "Injected via proxy"
        val injectedScenario = "Просмотр тест-кейса из подменённого ответа"
        val injectedPriority = Priority.MEDIUM.value

        val initialResponse = step("Открываем страницу и получаем исходный список тест-кейсов") {
            interceptResponseBody(selenideProxy, Paths.REPORTS.path) {
                step("Открываем главную страницу") { mainPage.open() }
            }
        }

        val reportResponse = JsonUtils.parse(initialResponse, TestReportResponse::class.java)
        val injectedTestCase = step("Готовим новый тест-кейс для отображения в списке") {
            TestReportItemDto(
                testId = injectedTestId,
                category = injectedCategory,
                shortTitle = injectedShortTitle,
                issueLink = "https://youtrack.test/issue/INJECT-1",
                readyDate = null,
                generalStatus = GeneralTestStatus.DONE.value,
                priority = injectedPriority,
                scenario = ScenarioRequest(steps = listOf(ScenarioStepRequest(number = 1, text = injectedScenario, attachments = emptyList()))),
                notes = "Добавлено через прокси",
                updatedAt = null,
            )
        }
        val modifiedResponse = step("Готовим обновлённый список с новым тест-кейсом") {
            reportResponse.copy(items = reportResponse.items + injectedTestCase)
        }

        step("Проверяем отсутствие нового тест-кейса в исходном списке") { mainPage.testCaseTable.checkRowDisappeared(injectedTestId) }

        replaceResponseBody(selenideProxy, Paths.REPORTS.path, JsonUtils.toJson(modifiedResponse)) {
            step("Обновляем страницу с новым списком тест-кейсов") { mainPage.refreshCurrentPage() }
            step("Проверяем отображение нового тест-кейса в обновлённом списке") { mainPage.testCaseTable.checkRowVisible(injectedTestId) }
        }
    }

}
