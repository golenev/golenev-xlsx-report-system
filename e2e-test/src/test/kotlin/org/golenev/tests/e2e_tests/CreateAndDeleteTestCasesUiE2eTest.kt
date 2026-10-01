package org.golenev.tests.e2e_tests

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner.getSelenideProxy
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.config.Paths
import org.golenev.restapi.endpoints.ReportServiceDao
import org.golenev.restapi.endpoints.TestUpsertItem
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.config.interceptRequestBody
import org.golenev.ui.pages.mainPage
import org.golenev.utils.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("E2E: Создание тест-кейсов через UI и удаление через API")
class CreateAndDeleteTestCasesUiE2eTest {

    private val reportService = ReportServiceDao()
    private val createdTestIds = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        step("Готовим приложение к работе") {
            DriverConfig().setup()
        }
    }

    @AfterEach
    fun tearDown() {
        step("Завершаем работу с приложением") {
            Selenide.closeWebDriver()
        }

        step("Удаляем тест-кейсы, если они остались после теста") {
            createdTestIds.forEach { testId -> TestReportDao.deleteByTestId(testId) }
        }
    }

    @Test
    @AllureId("302")
    @DisplayName("Создаём кейс через модальный редактор, удаляем через API и проверяем отсутствие")
    fun shouldCreateCaseViaModalAndDeleteItViaApi() {
        val readyDate = step("Фиксируем текущую дату для подготовки тест-кейсов") {
            LocalDate.now().toString()
        }
        val testCase = step("Готовим данные для тест-кейса") {
            val testId = "UI-E2E-${getRandomTestId()}"
            TestDataGenerator.generateTestCases(count = 1, readyDate = readyDate)
                .single()
                .copy(
                    testId = testId,
                    issueLink = "https://youtrack.test/issue/$testId",
                )
        }
        val testId = testCase.testId.orEmpty()
        createdTestIds += testId

        step("Открываем главную страницу") {
            mainPage.open()
        }

        step("Создаём тест-кейс через модальный редактор") {
            mainPage.testCaseTable.openCreateEditor()
            mainPage.testCaseTable.fillTestId(testId)
            mainPage.testCaseTable.fillCategory(testCase.category.orEmpty())
            mainPage.testCaseTable.fillShortTitle(testCase.shortTitle.orEmpty())
            mainPage.testCaseTable.fillIssueLink(testCase.issueLink.orEmpty())
            mainPage.testCaseTable.selectGeneralStatus(testCase.generalStatus.orEmpty())
            mainPage.testCaseTable.selectPriority(testCase.priority.orEmpty())
            mainPage.testCaseTable.fillDetailedScenarioSteps(testCase.scenario?.steps.orEmpty())
        }

        val createRequestBody = interceptRequestBody(getSelenideProxy(), Paths.REPORTS.path) {
            mainPage.testCaseTable.saveNewTestCase()
        }
        val actualCreateRequest = JsonUtils.parse(createRequestBody, TestUpsertItem::class.java)

        step("Проверяем данные, отправленные для добавления тест-кейса $testId") {
            actualCreateRequest.testId.shouldBe(testCase.testId, "actualCreateRequest.testId не совпало с ожидаемым")
            actualCreateRequest.category.shouldBe(testCase.category, "actualCreateRequest.category не совпало с ожидаемым")
            actualCreateRequest.shortTitle.shouldBe(testCase.shortTitle, "actualCreateRequest.shortTitle не совпало с ожидаемым")
            actualCreateRequest.issueLink.shouldBe(testCase.issueLink, "actualCreateRequest.issueLink не совпало с ожидаемым")
            actualCreateRequest.readyDate.shouldBe(testCase.readyDate, "actualCreateRequest.readyDate не совпало с ожидаемым")
            actualCreateRequest.generalStatus.shouldBe(testCase.generalStatus, "actualCreateRequest.generalStatus не совпало с ожидаемым")
            actualCreateRequest.priority.shouldBe(testCase.priority, "actualCreateRequest.priority не совпало с ожидаемым")
            actualCreateRequest.scenario.shouldBe(testCase.scenario, "actualCreateRequest.scenario не совпало с ожидаемым")
            actualCreateRequest.notes.orEmpty().shouldBe(testCase.notes, "actualCreateRequest.notes.orEmpty() не совпало с ожидаемым")
            actualCreateRequest.runStatus.shouldBe(testCase.runStatus, "actualCreateRequest.runStatus не совпало с ожидаемым")
            actualCreateRequest.runDate.shouldBe(testCase.runDate, "actualCreateRequest.runDate не совпало с ожидаемым")
        }

        step("Проверяем, что тест-кейс $testId появился в таблице") {
            mainPage.testCaseTable.checkRowVisible(testId)
        }

        step("Изменяем категорию тест-кейса $testId через модальный редактор") {
            mainPage.testCaseTable.openEditor(testId)
            mainPage.testCaseTable.updateCategory( "${testCase.category}-edited")
            mainPage.testCaseTable.saveChanges()
        }

        step("Удаляем тест-кейс $testId и обновляем страницу") {
            reportService.deleteTest(testId)
            mainPage.refreshCurrentPage()
            mainPage.testCaseTable.checkRowDisappeared(testId)
        }

        val remainingItems = step("Проверяем отсутствие тест-кейса $testId в списке тест-кейсов") {
            TestReportDao.countByTestId(testId)
        }

        step("Подтверждаем, что тест-кейс $testId отсутствует в списке тест-кейсов") {
            remainingItems.shouldBe(0, "remainingItems не совпало с ожидаемым")
        }
    }
}
