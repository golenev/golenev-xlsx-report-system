package com.example.e2e.tests.ui

import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestReportItemDto
import com.example.e2e.dto.TestReportResponse
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.http.Paths
import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.config.ProxyConfig
import com.example.e2e.ui.config.ProxyInitializer
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.JsonUtils
import com.example.e2e.utils.step
import com.codeborne.selenide.Selenide.closeWebDriver
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInfo
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UI: Работа с главной страницей")
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
            proxyThread = Thread(ProxyInitializer()).also {
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
    @AllureId("165")
    @DisplayName("Создание и удаление тест-кейса на главной странице")
    fun shouldCreateAndDeleteTestCase() {
        val testId = "UI-4565"
        val category = "UI smoke"
        val shortTitle = "Smoke title"
        val issueLink = "https://youtrack.test/issue/SMK-1"
        val generalStatus = "Готово"
        val detailedScenario = "Пользователь создаёт и удаляет тест-кейс"

        step("Открываем главную страницу") { mainPage.open() }
        step("Начинаем создание новой строки") { mainPage.startNewRow() }
        step("Заполняем поле Test ID значением $testId") { mainPage.fillTestId(testId) }
        step("Заполняем поле Category / Feature значением $category") { mainPage.fillCategory(category) }
        step("Заполняем поле Short Title значением $shortTitle") { mainPage.fillShortTitle(shortTitle) }
        step("Заполняем поле YouTrack Issue Link значением $issueLink") { mainPage.fillIssueLink(issueLink) }
        step("Выбираем значение General Test Status: $generalStatus") { mainPage.selectGeneralStatus(generalStatus) }
        step("Заполняем поле Detailed Scenario значением $detailedScenario") { mainPage.fillDetailedScenario(detailedScenario) }

        val requestBody = step("Сохраняем новую строку и перехватываем запрос") {
            ProxyConfig.interceptRequestBody(Paths.REPORTS.path) {
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

        val initialResponse = step("Открываем главную страницу и перехватываем первый ответ") {
            ProxyConfig.interceptResponseBody(Paths.REPORTS.path) {
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
                scenario = injectedScenario,
                notes = "Добавлено через прокси",
                updatedAt = null,
            )
        }
        val modifiedResponse = step("Формируем подменённый ответ с новым кейсом") {
            reportResponse.copy(items = reportResponse.items + injectedTestCase)
        }

        step("Проверяем, что тест-кейс отсутствует до подмены") { mainPage.shouldNotSeeTestCase(injectedTestId) }

        ProxyConfig.replaceResponseBody(Paths.REPORTS.path, JsonUtils.toJson(modifiedResponse)) {
            step("Обновляем страницу после подмены ответа") { mainPage.refreshCurrentPage() }
            step("Проверяем, что тест-кейс отображается после подмены") { mainPage.shouldSeeTestCase(injectedTestId) }
        }
    }

    @Test
    @AllureId("168")
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
