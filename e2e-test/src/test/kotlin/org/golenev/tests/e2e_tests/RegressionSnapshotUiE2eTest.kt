package org.golenev.tests.e2e_tests

import com.codeborne.selenide.Selenide
import io.kotest.matchers.nulls.shouldNotBeNull
import org.golenev.utils.shouldBe
import io.qameta.allure.AllureId
import org.golenev.db.tables.regression.RegressionDao
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.endpoints.ReportServiceDao
import org.golenev.restapi.endpoints.TestBatchRequest
import org.golenev.restapi.endpoints.TestUpsertItem
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.MainPage
import org.golenev.utils.TestDataGenerator
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDate
import kotlin.random.Random

@Isolated
@DisplayName("E2E: UI сценарий создания снапшота регресса")
class RegressionSnapshotUiE2eTest {

    private val mainPage = MainPage()
    private val reportService = ReportServiceDao()
    private lateinit var releaseName: String
    private val createdTestIds = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        step("Полностью очищаем таблицу тест-кейсов перед тестом") {
            TestReportDao.truncate()
        }

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
                RegressionDao.deleteByReleaseName(releaseName)
            }
        }

        step("Удаляем созданные тест-кейсы из базы, если они остались после теста") {
            createdTestIds.forEach { testId -> TestReportDao.deleteByTestId(testId) }
        }
    }

    @Test
    @AllureId("301")
    @DisplayName("Создаём тесты, запускаем регресс и проверяем снапшот в regressions.payload")
    fun shouldCreateSnapshotAfterCompletedRegression() {
        val readyDate = step("Фиксируем текущую дату для готовности тестов") {
            LocalDate.now().toString()
        }
        val testCases = step("Генерируем данные для двух тест-кейсов") {
            TestDataGenerator.generateTestCases(count = 2, readyDate = readyDate)
                .mapIndexed { index, testCase ->
                    val testId = "UI-REG-${getRandomTestId()}-${index + 1}"
                    testCase.copy(
                        testId = testId,
                        issueLink = "https://youtrack.test/issue/$testId",
                    )
                }
        }
        createdTestIds += testCases.mapNotNull { it.testId }

        val batchRequest = step("Готовим batch-запрос на создание двух тест-кейсов") {
            TestBatchRequest(items = testCases)
        }

        step("Создаём два тест-кейса через API") {
            reportService.sendBatch(batchRequest)
        }

        step("Открываем главную страницу") {
            mainPage.open()
        }

        step("Убеждаемся, что в таблице среди прочих отображаются две созданные записи") {
            testCases.forEach { testCase ->
                mainPage.shouldSeeTestCase(testCase.testId.orEmpty())
            }
        }

        releaseName = "ui-regression-${getRandomTestId()}"

        step("Запускаем регресс через UI с уникальным именем релиза") {
            mainPage.startRegression(releaseName)
        }

        val expectedStatuses = testCases
            .map { testCase -> testCase.testId.shouldNotBeNull() }
            .associateWith { RegressionRunStatus.entries.random(Random) }

        step("Проставляем случайные результаты прогона для двух тест-кейсов") {
            expectedStatuses.forEach { (testId, status) ->
                mainPage.selectRegressionStatus(testId, status.name)
            }
        }

        step("Завершаем регресс через UI") {
            mainPage.stopRegress()
        }

        val regression = step("Проверяем появление записи в таблице regressions") {
            RegressionDao.findByReleaseName(releaseName)
                .shouldNotBeNull()
        }

        step("Проверяем поля записи регресса") {
            regression.status.shouldBe("COMPLETED", "regression.status не совпало с ожидаемым")
            regression.releaseName.shouldBe(releaseName, "regression.releaseName не совпало с ожидаемым")
            regression.regressionDate.toString().shouldBe(readyDate, "regression.regressionDate.toString() не совпало с ожидаемым")
        }

        val payload = step("Проверяем, что снапшот записан в payload") {
            regression.payload.shouldNotBeNull()
        }

        step("Проверяем поля снапшота регресса") {
            payload.regressionDate.shouldBe(readyDate, "payload.regressionDate не совпало с ожидаемым")
            payload.status.shouldBe("COMPLETED", "payload.status не совпало с ожидаемым")
            payload.releaseName.shouldBe(releaseName, "payload.releaseName не совпало с ожидаемым")
        }

        val payloadTests = payload.tests.orEmpty()
        val payloadById = payloadTests
            .mapNotNull { test -> test.testId?.let { it to test } }
            .toMap()

        val expectedTests = testCases.map { testCase ->
            val testId = testCase.testId.shouldNotBeNull()
            testCase.copy(regressionStatus = expectedStatuses[testId]?.name)
        }

        step("Проверяем, что в снапшоте есть все два тест-кейса") {
            payloadTests.size.shouldBe(expectedTests.size, "payloadTests.size не совпало с ожидаемым")
            payloadById.keys.toSet().shouldBe(expectedTests.map { it.testId.shouldNotBeNull() }.toSet(), "payloadById.keys.toSet() не совпало с ожидаемым")
        }

        expectedTests.forEach { expected ->
            val expectedTestId = expected.testId.shouldNotBeNull()
            step("Проверяем колонки снапшота для теста ${expected.testId}") {
                val actual = payloadById[expectedTestId].shouldNotBeNull()
                actual.testId.shouldBe(expected.testId, "actual.testId не совпало с ожидаемым")
                actual.category.shouldBe(expected.category, "actual.category не совпало с ожидаемым")
                actual.shortTitle.shouldBe(expected.shortTitle, "actual.shortTitle не совпало с ожидаемым")
                actual.issueLink.shouldBe(expected.issueLink, "actual.issueLink не совпало с ожидаемым")
                actual.readyDate.shouldBe(expected.readyDate, "actual.readyDate не совпало с ожидаемым")
                actual.generalStatus.shouldBe(expected.generalStatus, "actual.generalStatus не совпало с ожидаемым")
                actual.priority.shouldBe(expected.priority, "actual.priority не совпало с ожидаемым")
                actual.scenario.shouldBe(expected.scenario, "actual.scenario не совпало с ожидаемым")
                actual.notes.shouldBe(expected.notes, "actual.notes не совпало с ожидаемым")
                actual.runStatus.shouldBe(expected.runStatus, "actual.runStatus не совпало с ожидаемым")
                actual.runDate.shouldBe(expected.runDate, "actual.runDate не совпало с ожидаемым")
                actual.regressionStatus.shouldBe(expected.regressionStatus, "actual.regressionStatus не совпало с ожидаемым")
            }
        }
    }
}

private enum class RegressionRunStatus {
    PASSED,
    FAILED,
    SKIPPED,
}
