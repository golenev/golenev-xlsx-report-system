package org.golenev.tests.e2e_tests

import com.codeborne.selenide.Selenide
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.MainPage
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

                mainPage.fillDetailedScenarioSteps(testCase.scenario.toUiSteps())
                mainPage.shouldEnableSaveNewRow()
                mainPage.shouldEnableAddRow()
                mainPage.saveNewRow()
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
                remainingItems shouldBe 0
            }
        }
    }

    private fun org.golenev.restapi.endpoints.ScenarioRequest?.toUiSteps(): List<String> =
        this?.steps.orEmpty().mapNotNull { step -> step.text }
}
