package org.golenev.tests.backend

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.endpoints.ReportServiceDao
import org.golenev.restapi.endpoints.TestBatchRequest
import org.golenev.utils.TestDataGenerator.generateTestCases
import org.golenev.utils.shouldBe
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Апи тест отправки батча тест кейсов через апи")
class TestSendForceBatchApiTest {

    private val reportService = ReportServiceDao()
    private lateinit var batchRequest: TestBatchRequest
    val reportDay: LocalDate = LocalDate.now().minusDays(18)

    @AfterEach
    fun cleaDb() {
        step("Удаляем все созданные тест-кейсы") {
            TestReportDao.deleteReportsByDate(reportDay)
        }
    }

    @AllureId("169")
    @Test
    @DisplayName("Создаем запись через batch и проверяем отображение в отчете")
    fun createAndReadReportThroughApi() {
        step("Удаляем тест-кейсы с датой готовности $reportDay — 18 дней назад") {
            TestReportDao.deleteReportsByDate(reportDay)
        }

        batchRequest = TestBatchRequest(
            items = generateTestCases(10, readyDate = reportDay.toString()),
        )

        step("Добавляем десять тест-кейсов с датой готовности $reportDay — 18 дней назад, в категории «E2E_FOR_AUTOTEST», со статусом «Готово» и приоритетом «Medium»") {
            reportService.sendForceBatch(batchRequest)
        }

        val report = step("Запрашиваем отчет о тестах") {
            reportService.getReport()
        }

        step("Проверяем, что за $reportDay отображаются все десять добавленных тест-кейсов") {
            report.items
                .filter { it.readyDate == reportDay }
                .shouldHaveSize(batchRequest.items.size)
        }

        val itemsById = report.items.associateBy { it.testId }

        step("Проверяем сохранение всех подготовленных тест-кейсов") {
            batchRequest.items.forEach {
                val testId = it.testId.shouldNotBeNull()
                val reportItem = itemsById[testId].shouldNotBeNull()
                reportItem.category.shouldBe(it.category, "reportItem.category не совпало с ожидаемым")
                reportItem.shortTitle.shouldBe(it.shortTitle, "reportItem.shortTitle не совпало с ожидаемым")
                reportItem.readyDate.shouldBe(reportDay, "reportItem.readyDate не совпало с ожидаемым")
                reportItem.generalStatus.shouldBe(it.generalStatus, "reportItem.generalStatus не совпало с ожидаемым")
                reportItem.priority.shouldBe(it.priority, "reportItem.priority не совпало с ожидаемым")
                reportItem.updatedAt.shouldNotBeNull()
            }
        }
    }
}

