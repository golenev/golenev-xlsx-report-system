package com.example.e2e.tests.backend

import com.example.e2e.db.DatabaseCleaner
import com.example.e2e.db.TestReportTable
import com.example.e2e.db.dbReportExec
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.service.ReportService
import com.example.e2e.utils.TestDataGenerator.generateTestCases
import com.example.e2e.utils.step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestReportE2ETest {

    private val reportService = ReportService()
    private lateinit var batchRequest: TestBatchRequest

    @AfterEach
    fun cleaDb() {
       val items = batchRequest.items.map { it.testId.shouldNotBeNull() }

        items.forEach { item->
            dbReportExec {
                TestReportTable.deleteWhere { TestReportTable.testId eq item }
            }
        }
    }

    @AllureId("169")
    @Test
    @DisplayName("Создаем запись через batch и проверяем отображение в отчете")
    fun createAndReadReportThroughApi() {
        val today = step("Определяем дату запуска теста") { LocalDate.now() }

        step("Удаляем отчеты за выбранную дату") {
            DatabaseCleaner.deleteReportsByDate(today)
        }

        batchRequest = step("Формируем batch-запрос с десятью тестами") {
            TestBatchRequest(
                items = generateTestCases(10),
            )
        }

        step("Отправляем batch на обновление тестов") {
            reportService.sendBatch(batchRequest)
        }

        val report = step("Запрашиваем отчет о тестах") {
            reportService.getReport()
        }

        step("Проверяем количество записей за выбранную дату") {
            report.items
                .filter { it.readyDate == today }
                .shouldHaveSize(batchRequest.items.size)
        }

        // Создаём удобную мапу по testId
        val itemsById = report.items.associateBy { it.testId }

        step("Проверяем все записи из batch-запроса") {
            batchRequest.items.forEach {
                val testId = it.testId.shouldNotBeNull()
                val reportItem = itemsById[testId].shouldNotBeNull()
                reportItem.category shouldBe it.category
                reportItem.shortTitle shouldBe it.shortTitle
                reportItem.readyDate shouldBe today
                reportItem.generalStatus shouldBe it.generalStatus
                reportItem.updatedAt.shouldNotBeNull()
            }
        }
    }
}

