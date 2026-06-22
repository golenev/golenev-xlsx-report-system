package org.golenev.tests.backend

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import org.golenev.utils.shouldBe
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.endpoints.ReportServiceDao
import org.golenev.restapi.endpoints.TestBatchRequest
import org.golenev.utils.TestDataGenerator.generateTestCases
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Апи тест отправки батча тест кейсов через апи")
class TestSendForceBatchApiTest {

    private val reportService = ReportServiceDao()
    private lateinit var batchRequest: TestBatchRequest
    val reportDay: LocalDate =
        step("Определяем дату запуска теста") { LocalDate.now().minusDays(18) }

    @AfterEach
    fun cleaDb() {
        step("Удаление всех созданных тест кейсов из базы") {
            TestReportDao.deleteReportsByDate(reportDay)
        }
    }

    @AllureId("169")
    @Test
    @DisplayName("Создаем запись через batch и проверяем отображение в отчете")
    fun createAndReadReportThroughApi() {
        step("Удаляем отчеты за выбранную дату") {
            TestReportDao.deleteReportsByDate(reportDay)
        }

        batchRequest = step("Формируем batch-запрос с десятью тестами") {
            TestBatchRequest(
                items = generateTestCases(10, readyDate = reportDay.toString()),
            )
        }

        step("Отправляем batch на обновление тестов") {
            reportService.sendForceBatch(batchRequest)
        }

        val report = step("Запрашиваем отчет о тестах") {
            reportService.getReport()
        }

        step("Проверяем количество записей за выбранную дату") {
            report.items
                .filter { it.readyDate == reportDay }
                .shouldHaveSize(batchRequest.items.size)
        }

        val itemsById = report.items.associateBy { it.testId }

        step("Проверяем все записи из batch-запроса") {
            batchRequest.items.forEach {
                val testId = it.testId.shouldNotBeNull()
                val reportItem = itemsById[testId].shouldNotBeNull()
                reportItem.category.shouldBe(it.category, "Проверяем, что reportItem.category равно it.category")
                reportItem.shortTitle.shouldBe(it.shortTitle, "Проверяем, что reportItem.shortTitle равно it.shortTitle")
                reportItem.readyDate.shouldBe(reportDay, "Проверяем, что reportItem.readyDate равно reportDay")
                reportItem.generalStatus.shouldBe(it.generalStatus, "Проверяем, что reportItem.generalStatus равно it.generalStatus")
                reportItem.priority.shouldBe(it.priority, "Проверяем, что reportItem.priority равно it.priority")
                reportItem.updatedAt.shouldNotBeNull()
            }
        }
    }
}

