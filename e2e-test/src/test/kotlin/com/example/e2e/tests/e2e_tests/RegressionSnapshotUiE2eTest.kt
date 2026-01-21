package com.example.e2e.tests.e2e_tests

import com.codeborne.selenide.Selenide
import com.example.e2e.db.dbReportExec
import com.example.e2e.db.repository.RegressionRepository
import com.example.e2e.db.tables.TestReportTable
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.Priority
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.TestDataGenerator
import com.example.e2e.utils.getRandomTestId
import com.example.e2e.utils.step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.jetbrains.exposed.sql.deleteAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("E2E: UI сценарий создания снапшота регресса")
class RegressionSnapshotUiE2eTest {

    private val mainPage = MainPage()
    private val reportService = ReportService()
    private lateinit var releaseName: String

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

        step("Удаляем созданный регресс из базы") {
            if (::releaseName.isInitialized) {
                RegressionRepository.deleteByReleaseName(releaseName)
            }
        }

        step("Очищаем таблицу тест кейсов") {
            dbReportExec {
                TestReportTable.deleteAll()
            }
        }
    }

    @Test
    @AllureId("301")
    @DisplayName("Создаём тесты, запускаем регресс и проверяем снапшот в regressions.payload")
    fun shouldCreateSnapshotAfterCompletedRegression() {
        val readyDate = step("Фиксируем текущую дату для готовности тестов") {
            LocalDate.now().toString()
        }
        val generatedTests = step("Генерируем 10 тестовых сценариев") {
            TestDataGenerator.generateTestCases(count = 10, readyDate = readyDate)
        }
        val batchRequest = step("Готовим batch-запрос на создание 10 тестов") {
            TestBatchRequest(items = generatedTests)
        }

        step("Создаём 10 тестов через API") {
            reportService.sendBatch(batchRequest)
        }

        step("Открываем главную страницу") {
            mainPage.open()
        }

        step("Убеждаемся, что добавлено 10 новых строк") {
            mainPage.shouldHaveTestCasesCount(10)
        }

        val manualTestId = "UI-REG-${getRandomTestId()}"
        val manualCategory = "UI regression"
        val manualShortTitle = "Manual regression case"
        val manualIssueLink = "https://youtrack.test/issue/$manualTestId"
        val manualGeneralStatus = GeneralTestStatus.DONE.value
        val manualPriority = Priority.HIGH.value
        val manualScenario = "Ручной тест-кейс для регрессионного прогона"
        val manualNotes = "Ручная запись для проверки снапшота"

        step("Добавляем вручную 11-ю строку через UI") {
            mainPage.startNewRow()
            mainPage.fillTestId(manualTestId)
            mainPage.fillCategory(manualCategory)
            mainPage.fillShortTitle(manualShortTitle)
            mainPage.fillIssueLink(manualIssueLink)
            mainPage.selectGeneralStatus(manualGeneralStatus)
            mainPage.selectPriority(manualPriority)
            mainPage.fillDetailedScenario(manualScenario)
            mainPage.fillNotes(manualNotes)
            mainPage.saveNewRow()
        }

        step("Убеждаемся, что 11-й тест отображается в таблице") {
            mainPage.shouldSeeTestCase(manualTestId)
        }

        releaseName = "ui-regression-${getRandomTestId()}"

        step("Запускаем регресс через UI с уникальным именем релиза") {
            mainPage.startRegression(releaseName)
        }

        val regressionStatuses = listOf("PASSED", "FAILED", "SKIPPED")
        val expectedStatuses = (generatedTests.map { it.testId.shouldNotBeNull() } + manualTestId)
            .mapIndexed { index, testId -> testId to regressionStatuses[index % regressionStatuses.size] }
            .toMap()

        step("Проставляем результаты прогона для всех 11 тестов") {
            expectedStatuses.forEach { (testId, status) ->
                mainPage.selectRegressionStatus(testId, status)
            }
        }

        step("Завершаем регресс через UI") {
            mainPage.stopRegress()
        }

        val regression = step("Проверяем появление записи в таблице regressions") {
            RegressionRepository.findByReleaseName(releaseName).shouldNotBeNull()
        }

        step("Проверяем поля записи регресса") {
            regression.status shouldBe "COMPLETED"
            regression.releaseName shouldBe releaseName
            regression.regressionDate.toString() shouldBe readyDate
        }

        val payload = step("Проверяем, что снапшот записан в payload") {
            regression.payload.shouldNotBeNull()
        }

        step("Проверяем поля снапшота регресса") {
            payload.regressionDate shouldBe readyDate
            payload.status shouldBe "COMPLETED"
            payload.releaseName shouldBe releaseName
        }

        val payloadTests = payload.tests.orEmpty()
        val payloadById = payloadTests
            .mapNotNull { test -> test.testId?.let { it to test } }
            .toMap()

        val expectedTests = generatedTests.map { it.copy() } + TestUpsertItem(
            testId = manualTestId,
            category = manualCategory,
            shortTitle = manualShortTitle,
            issueLink = manualIssueLink,
            readyDate = readyDate,
            generalStatus = manualGeneralStatus,
            priority = manualPriority,
            scenario = manualScenario,
            notes = manualNotes,
        )

        step("Проверяем, что в снапшоте есть все 11 тестов") {
            payloadTests.size shouldBe expectedTests.size
            payloadById.keys.toSet() shouldBe expectedTests.map { it.testId.shouldNotBeNull() }.toSet()
        }

        expectedTests.forEach { expected ->
            val expectedTestId = expected.testId.shouldNotBeNull()
            step("Проверяем колонки снапшота для теста ${expected.testId}") {
                val actual = payloadById[expectedTestId].shouldNotBeNull()
                actual.testId shouldBe expected.testId
                actual.category shouldBe expected.category
                actual.shortTitle shouldBe expected.shortTitle
                actual.issueLink shouldBe expected.issueLink
                actual.readyDate shouldBe expected.readyDate
                actual.generalStatus shouldBe expected.generalStatus
                actual.priority shouldBe expected.priority
                actual.scenario shouldBe expected.scenario
                actual.notes shouldBe expected.notes
            }
        }
    }
}
