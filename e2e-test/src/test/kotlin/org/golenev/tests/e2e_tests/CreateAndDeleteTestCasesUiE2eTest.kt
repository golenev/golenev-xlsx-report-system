package org.golenev.tests.e2e_tests

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner.getSelenideProxy
import org.golenev.utils.shouldBe
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.config.Paths
import org.golenev.restapi.endpoints.TestUpsertItem
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.config.interceptRequestBody
import org.golenev.ui.pages.MainPage
import org.golenev.utils.JsonUtils
import org.golenev.utils.TestDataGenerator
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("E2E: Создание и удаление тест-кейсов через UI")
class CreateAndDeleteTestCasesUiE2eTest {

    private val mainPage = MainPage()
    private val createdTestIds = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        step("Настраиваем драйвер Selenide") {
            DriverConfig().setup()
        }
    }

    @AfterEach
    fun tearDown() {
        step("Закрываем веб-драйвер") {
            Selenide.closeWebDriver()
        }

        step("Удаляем тест-кейсы из базы, если они остались после теста") {
            createdTestIds.forEach { testId -> TestReportDao.deleteByTestId(testId) }
        }
    }

    @Test
    @AllureId("302")
    @DisplayName("Создаём два кейса через UI, проверяем блокировки, удаляем через UI и проверяем отсутствие")
    fun shouldCreateTwoCasesWithUiValidationAndDeleteThemViaUi() {
        val readyDate = step("Фиксируем текущую дату для генерации тест-кейсов") {
            LocalDate.now().toString()
        }
        val testCases = step("Генерируем данные для двух тест-кейсов") {
            TestDataGenerator.generateTestCases(count = 2, readyDate = readyDate)
                .mapIndexed { index, testCase ->
                    val testId = "UI-E2E-${getRandomTestId()}-${index + 1}"
                    testCase.copy(
                        testId = testId,
                        issueLink = "https://youtrack.test/issue/$testId",
                    )
                }
        }
        createdTestIds += testCases.mapNotNull { it.testId }

        step("Открываем главную страницу") {
            mainPage.open()
        }

        testCases.forEachIndexed { index, testCase ->
            step("Создаём тест-кейс ${index + 1} через UI и проверяем блокировки кнопок") {
                mainPage.startNewRow()
                mainPage.shouldDisableAddRow()
                mainPage.shouldDisableSaveNewRow()

                mainPage.fillTestId(testCase.testId.orEmpty())
                mainPage.shouldDisableAddRow()
                mainPage.shouldDisableSaveNewRow()

                mainPage.fillCategory(testCase.category.orEmpty())
                mainPage.fillShortTitle(testCase.shortTitle.orEmpty())
                mainPage.fillIssueLink(testCase.issueLink.orEmpty())
                mainPage.selectGeneralStatus(testCase.generalStatus.orEmpty())
                mainPage.selectPriority(testCase.priority.orEmpty())
                mainPage.shouldDisableAddRow()
                mainPage.shouldDisableSaveNewRow()

                mainPage.fillDetailedScenarioSteps(testCase.scenario?.steps.orEmpty())
                mainPage.shouldEnableSaveNewRow()
                mainPage.shouldEnableAddRow()

                val createRequestBody = interceptRequestBody(getSelenideProxy(), Paths.REPORTS.path) {
                    mainPage.saveNewRow()
                }
                val actualCreateRequest = JsonUtils.parse(createRequestBody, TestUpsertItem::class.java)

                step("Проверяем тело запроса создания тест-кейса ${testCase.testId}") {
                    actualCreateRequest.testId.shouldBe(testCase.testId, "Проверяем, что actualCreateRequest.testId равно testCase.testId")
                    actualCreateRequest.category.shouldBe(testCase.category, "Проверяем, что actualCreateRequest.category равно testCase.category")
                    actualCreateRequest.shortTitle.shouldBe(testCase.shortTitle, "Проверяем, что actualCreateRequest.shortTitle равно testCase.shortTitle")
                    actualCreateRequest.issueLink.shouldBe(testCase.issueLink, "Проверяем, что actualCreateRequest.issueLink равно testCase.issueLink")
                    actualCreateRequest.readyDate.shouldBe(testCase.readyDate, "Проверяем, что actualCreateRequest.readyDate равно testCase.readyDate")
                    actualCreateRequest.generalStatus.shouldBe(testCase.generalStatus, "Проверяем, что actualCreateRequest.generalStatus равно testCase.generalStatus")
                    actualCreateRequest.priority.shouldBe(testCase.priority, "Проверяем, что actualCreateRequest.priority равно testCase.priority")
                    actualCreateRequest.scenario.shouldBe(testCase.scenario, "Проверяем, что actualCreateRequest.scenario равно testCase.scenario")
                    actualCreateRequest.notes.orEmpty().shouldBe(testCase.notes, "Проверяем, что actualCreateRequest.notes.orEmpty() равно testCase.notes")
                    actualCreateRequest.runStatus.shouldBe(testCase.runStatus, "Проверяем, что actualCreateRequest.runStatus равно testCase.runStatus")
                    actualCreateRequest.runDate.shouldBe(testCase.runDate, "Проверяем, что actualCreateRequest.runDate равно testCase.runDate")
                }
            }

            step("Проверяем, что тест-кейс ${testCase.testId} появился на UI") {
                mainPage.shouldSeeTestCase(testCase.testId.orEmpty())
            }
        }

        testCases.forEachIndexed { index, testCase ->
            val testId = testCase.testId.orEmpty()
            val updatedCategory = "${testCase.category}-edited-${index + 1}"

            step("Редактируем Category тест-кейса $testId и проверяем блокировку Add Row") {
                mainPage.updateCategory(testId, updatedCategory)
                mainPage.shouldDisableAddRow()
                mainPage.unFocus()
                mainPage.shouldEnableAddRow()
            }
        }

        testCases.forEach { testCase ->
            val testId = testCase.testId.orEmpty()

            step("Удаляем тест-кейс $testId через UI") {
                mainPage.deleteTestCase(testId)
                mainPage.shouldNotSeeTestCase(testId)
            }

            val remainingItems = step("Проверяем отсутствие тест-кейса $testId в базе данных") {
                TestReportDao.countByTestId(testId)
            }

            step("Подтверждаем, что тест-кейс $testId отсутствует в базе") {
                remainingItems.shouldBe(0, "Проверяем, что remainingItems равно 0")
            }
        }
    }
}
