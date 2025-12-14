package com.example.e2e.tests.backend

import com.example.e2e.db.repository.TestReportRepository
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.service.ReportService
import com.example.e2e.utils.TestDataGenerator.generateTestCases
import com.example.e2e.utils.step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestReportE2ETest {

    private val reportService = ReportService()
    private lateinit var batchRequest: TestBatchRequest
    val reportDay: LocalDate = step("Определяем дату запуска теста") { LocalDate.now().minusDays(18) }

    @AfterEach
    fun cleaDb() {
        step("Удаление всех созданных тест кейсов из базы") {
            TestReportRepository.deleteReportsByDate(reportDay)
        }

    }

    @AllureId("169")
    @Test
    @DisplayName("Создаем запись через batch и проверяем отображение в отчете")
    fun createAndReadReportThroughApi() {
        step("Удаляем отчеты за выбранную дату") {
            TestReportRepository.deleteReportsByDate(reportDay)
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
                reportItem.category shouldBe it.category
                reportItem.shortTitle shouldBe it.shortTitle
                reportItem.readyDate shouldBe reportDay
                reportItem.generalStatus shouldBe it.generalStatus
                reportItem.priority shouldBe it.priority
                reportItem.updatedAt.shouldNotBeNull()
            }
        }
    }
}

