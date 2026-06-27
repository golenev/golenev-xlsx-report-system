package org.golenev.tests.e2e_tests

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Condition.disabled
import com.codeborne.selenide.Condition.enabled
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.ScrollIntoViewOptions.Block.start
import com.codeborne.selenide.ScrollIntoViewOptions.instant
import org.golenev.ui.pages.Application.testCaseTable
import org.golenev.utils.CENTER
import com.codeborne.selenide.WebDriverRunner.getSelenideProxy
import org.golenev.utils.shouldBe
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.config.Paths
import org.golenev.restapi.endpoints.TestUpsertItem
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.config.interceptRequestBody
import org.golenev.ui.pages.Application.mainPage
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
                testCaseTable.addRowButton
                    .scrollIntoView(instant().block(start))
                    .shouldBe(disabled.because("кнопка Add Row должна быть недоступна, пока форма создания строки не готова к сохранению"))
                testCaseTable.draftRow.saveButton
                    .shouldBe(disabled.because("кнопка сохранения draft-строки должна быть недоступна, пока форма создания строки не готова к сохранению"))

                mainPage.fillTestId(testCase.testId.orEmpty())
                testCaseTable.addRowButton
                    .scrollIntoView(instant().block(start))
                    .shouldBe(disabled.because("кнопка Add Row должна быть недоступна, пока форма создания строки не готова к сохранению"))
                testCaseTable.draftRow.saveButton
                    .shouldBe(disabled.because("кнопка сохранения draft-строки должна быть недоступна, пока форма создания строки не готова к сохранению"))

                mainPage.fillCategory(testCase.category.orEmpty())
                mainPage.fillShortTitle(testCase.shortTitle.orEmpty())
                mainPage.fillIssueLink(testCase.issueLink.orEmpty())
                mainPage.selectGeneralStatus(testCase.generalStatus.orEmpty())
                mainPage.selectPriority(testCase.priority.orEmpty())
                testCaseTable.addRowButton
                    .scrollIntoView(instant().block(start))
                    .shouldBe(disabled.because("кнопка Add Row должна быть недоступна, пока форма создания строки не готова к сохранению"))
                testCaseTable.draftRow.saveButton
                    .shouldBe(disabled.because("кнопка сохранения draft-строки должна быть недоступна, пока форма создания строки не готова к сохранению"))

                mainPage.fillDetailedScenarioSteps(testCase.scenario?.steps.orEmpty())
                testCaseTable.draftRow.saveButton
                    .shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей"))
                testCaseTable.addRowButton
                    .shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса"))

                val createRequestBody = interceptRequestBody(getSelenideProxy(), Paths.REPORTS.path) {
                    mainPage.saveNewRow()
                }
                val actualCreateRequest = JsonUtils.parse(createRequestBody, TestUpsertItem::class.java)

                step("Проверяем тело запроса создания тест-кейса ${testCase.testId}") {
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
            }

            step("Проверяем, что тест-кейс ${testCase.testId} появился на UI") {
                testCaseTable.row(testCase.testId.orEmpty()).root
                    .scrollIntoView(CENTER)
                    .shouldBe(visible.because("строка тест-кейса должна быть видимой на странице после прокрутки"))
            }
        }

        testCases.forEachIndexed { index, testCase ->
            val testId = testCase.testId.orEmpty()
            val updatedCategory = "${testCase.category}-edited-${index + 1}"

            step("Редактируем Category тест-кейса $testId и проверяем блокировку Add Row") {
                mainPage.updateCategory(testId, updatedCategory)
                testCaseTable.addRowButton
                    .scrollIntoView(instant().block(start))
                    .shouldBe(disabled.because("кнопка Add Row должна быть недоступна, пока форма создания строки не готова к сохранению"))
                mainPage.unFocus()
                testCaseTable.addRowButton
                    .shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса"))
            }
        }

        testCases.forEach { testCase ->
            val testId = testCase.testId.orEmpty()

            step("Удаляем тест-кейс $testId через UI") {
                mainPage.deleteTestCase(testId)
                testCaseTable.row(testId).root
                    .shouldBe(com.codeborne.selenide.Condition.disappear.because("строка тест-кейса должна исчезнуть после выполненного действия"))
            }

            val remainingItems = step("Проверяем отсутствие тест-кейса $testId в базе данных") {
                TestReportDao.countByTestId(testId)
            }

            step("Подтверждаем, что тест-кейс $testId отсутствует в базе") {
                remainingItems.shouldBe(0, "remainingItems не совпало с ожидаемым")
            }
        }
    }
}
