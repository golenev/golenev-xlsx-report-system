package com.example.e2e.tests.ui

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner.getSelenideProxy
import com.codeborne.selenide.proxy.SelenideProxyServer
import com.example.e2e.dto.*
import com.example.e2e.http.Paths
import com.example.e2e.ui.config.*
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.JsonUtils
import com.example.e2e.utils.step
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.junit.jupiter.api.*

@DisplayName("UI: Создание записей на главной странице")
class CreateTestCaseUIE2eProxyTests {

    private val mainPage = MainPage()
    private lateinit var selenideProxy: SelenideProxyServer

    @BeforeEach
    fun setUpProxy() {
        DriverConfig().setup()
        mainPage.open()
        selenideProxy = getSelenideProxy()
    }

    @AfterEach
    fun tearDown() {
        step("Закрываем драйвер и прокси") {
            Selenide.closeWebDriver()
        }
    }

    @Test
    @AllureId("165")
    @DisplayName("Создание и удаление тест-кейса на главной странице")
    fun shouldCreateAndDeleteTestCase() {
        val testId = "UI-4565"
        val category = "UI smoke"
        val shortTitle = "Smoke title"
        val issueLink = "https://youtrack.test/issue/SMK-1"
        val generalStatus = "Готово"
        val priority = Priority.HIGH.value
        val detailedScenario = "Пользователь создаёт и удаляет тест-кейс"

        step("Открываем главную страницу") { mainPage.open() }
        step("Начинаем создание новой строки") { mainPage.startNewRow() }
        step("Заполняем поле Test ID значением $testId") { mainPage.fillTestId(testId) }
        step("Заполняем поле Category / Feature значением $category") { mainPage.fillCategory(category) }
        step("Заполняем поле Short Title значением $shortTitle") { mainPage.fillShortTitle(shortTitle) }
        step("Заполняем поле YouTrack Issue Link значением $issueLink") { mainPage.fillIssueLink(issueLink) }
        step("Выбираем значение General Test Status: $generalStatus") { mainPage.selectGeneralStatus(generalStatus) }
        step("Выбираем значение Priority: $priority") { mainPage.selectPriority(priority) }
        step("Заполняем поле Detailed Scenario значением $detailedScenario") {
            mainPage.fillDetailedScenario(
                detailedScenario
            )
        }

        val requestBody = step("Сохраняем новую строку и перехватываем запрос") {
            interceptRequestBody(selenideProxy, Paths.REPORTS.path) {
                step("Сохраняем новую строку") { mainPage.saveNewRow() }
            }
        }

        val createdTest = JsonUtils.parse(requestBody, TestUpsertItem::class.java)

        step("Проверяем, что запрос на сохранение тест-кейса содержит корректные данные") {
            assertSoftly {
                createdTest.testId shouldBe testId
                createdTest.category shouldBe category
                createdTest.shortTitle shouldBe shortTitle
                createdTest.issueLink shouldBe issueLink
                createdTest.generalStatus shouldBe generalStatus
                createdTest.priority shouldBe priority
                createdTest.scenario shouldBe detailedScenario
            }
        }

        step("Проверяем, что созданный тест-кейс отображается в таблице") { mainPage.shouldSeeTestCase(testId) }
        step("Удаляем созданный тест-кейс") { mainPage.deleteTestCase(testId) }
        step("Убеждаемся, что тест-кейс удалён") { mainPage.shouldNotSeeTestCase(testId) }
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

        val initialResponse = step("Открываем главную страницу и перехватываем первый ответ") {
            interceptResponseBody(selenideProxy, Paths.REPORTS.path) {
                step("Открываем главную страницу") { mainPage.open() }
            }
        }

        val reportResponse = JsonUtils.parse(initialResponse, TestReportResponse::class.java)
        val injectedTestCase = step("Готовим тестовый кейс для подмены ответа") {
            TestReportItemDto(
                testId = injectedTestId,
                category = injectedCategory,
                shortTitle = injectedShortTitle,
                issueLink = "https://youtrack.test/issue/INJECT-1",
                readyDate = null,
                generalStatus = GeneralTestStatus.DONE.value,
                priority = injectedPriority,
                scenario = injectedScenario,
                notes = "Добавлено через прокси",
                updatedAt = null,
            )
        }
        val modifiedResponse = step("Формируем подменённый ответ с новым кейсом") {
            reportResponse.copy(items = reportResponse.items + injectedTestCase)
        }

        step("Проверяем, что тест-кейс отсутствует до подмены") { mainPage.shouldNotSeeTestCase(injectedTestId) }

        replaceResponseBody(selenideProxy, Paths.REPORTS.path, JsonUtils.toJson(modifiedResponse)) {
            step("Обновляем страницу после подмены ответа") { mainPage.refreshCurrentPage() }
            step("Проверяем, что тест-кейс отображается после подмены") { mainPage.shouldSeeTestCase(injectedTestId) }
        }
    }

}