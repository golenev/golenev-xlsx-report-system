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
        DriverConfig().setup()
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
        val readyDate = LocalDate.now().toString()
        val testCase = run {
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
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }

        step("Открываем модальное окно создания тест-кейса") {
            mainPage.header.openCreateEditor()
        }
        step("Проверяем открытие редактора создания") { mainPage.testCaseEditor.checkVisible() }
        step("Проверяем готовность режима создания") { mainPage.testCaseEditor.checkCreateModeReady() }
        step("Заполняем тест-кейс $testId за сегодня: категория ${testCase.category}, статус «${testCase.generalStatus}», приоритет «${testCase.priority}», сценарий из ${testCase.scenario?.steps.orEmpty().size} шагов") {
            mainPage.testCaseEditor.fillTestId(testId)
            mainPage.testCaseEditor.fillCategory(testCase.category.orEmpty())
            mainPage.testCaseEditor.fillShortTitle(testCase.shortTitle.orEmpty())
            mainPage.testCaseEditor.fillIssueLink(testCase.issueLink.orEmpty())
            mainPage.testCaseEditor.selectGeneralStatus(testCase.generalStatus.orEmpty())
            mainPage.testCaseEditor.selectPriority(testCase.priority.orEmpty())
        }
        (testCase.scenario?.steps.orEmpty()).forEachIndexed { index, scenarioStep ->
            if (index > 0) {
                step("Добавляем корневой шаг сценария") { mainPage.testCaseEditor.scenarioEditor.addRootStep() }
            }
            step("Проверяем шаг ${scenarioStep.number} сценария") { mainPage.testCaseEditor.scenarioEditor.checkStepVisible(index, scenarioStep.number) }
            step("Заполняем текст шага ${scenarioStep.number}") { mainPage.testCaseEditor.scenarioEditor.fillStepText(index, scenarioStep.text) }
            scenarioStep.attachments.firstOrNull { it.content.isNotBlank() }?.let { attachment ->
                step("Раскрываем шаг сценария") { mainPage.testCaseEditor.scenarioEditor.expandStep(index) }
                step("Добавляем вложение шага") { mainPage.testCaseEditor.scenarioEditor.addStepAttachment(index) }
                step("Указываем имя вложения") { mainPage.testCaseEditor.scenarioEditor.fillAttachmentName(index, attachment.name) }
                step("Раскрываем вложение") { mainPage.testCaseEditor.scenarioEditor.expandAttachment(index) }
                step("Заполняем содержимое вложения") { mainPage.testCaseEditor.scenarioEditor.fillAttachmentContent(index, attachment.content.trim()) }
            }
        }

        val createRequestBody = interceptRequestBody(getSelenideProxy(), Paths.REPORTS.path) {
            step("Сохраняем тест-кейс $testId с датой готовности за сегодня") { mainPage.testCaseEditor.footer.saveNewTestCase() }
            step("Проверяем закрытие редактора") { mainPage.testCaseEditor.checkEditorClosed() }
        }
        val actualCreateRequest = JsonUtils.parse(createRequestBody, TestUpsertItem::class.java)

        step("Проверяем сохранение тест-кейса $testId за сегодня: идентификатор, категория, название, задача, статус, приоритет, шаги сценария и вложения совпадают с введёнными") {
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

        step("Открываем модальное окно изменения тест-кейса") {
            mainPage.testCaseTable.openEditor(testId)
        }
        step("Проверяем открытие редактора изменения") { mainPage.testCaseEditor.checkVisible() }
        step("Проверяем выбранный тест-кейс и режим изменения") { mainPage.testCaseEditor.checkEditModeReady(testId) }
        step("Изменяем категорию тест-кейса $testId через модальный редактор") {
            mainPage.testCaseEditor.updateCategory( "${testCase.category}-edited")
            mainPage.testCaseEditor.footer.saveChanges()
        }
        step("Проверяем закрытие редактора") { mainPage.testCaseEditor.checkEditorClosed() }

        step("Удаляем тест-кейс $testId и обновляем страницу") {
            reportService.deleteTest(testId)
            mainPage.refreshCurrentPage()
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Проверяем отсутствие строки тест-кейса") {
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
