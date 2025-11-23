package com.example.e2e.tests

import com.example.e2e.db.DatabaseCleaner
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import io.qameta.allure.Allure.step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestReportE2ETest {

    private val reportService = ReportService()

    @Test
    @DisplayName("Создаем запись через batch и проверяем отображение в отчете")
    fun createAndReadReportThroughApi() {
        val today = step("Определяем дату запуска теста") { LocalDate.now() }

        step("Удаляем отчеты за выбранную дату") {
            DatabaseCleaner.deleteReportsByDate(today)
        }

        val batchRequest = step("Формируем batch-запрос с двумя тестами") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "45-7",
                        category = "E2E",
                        shortTitle = "Smoke отчет 1",
                        scenario = "Короткий сценарий 1",
                        readyDate = today.toString(),
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runIndex = 1,
                        runStatus = "PASSED",
                        runDate = today.toString(),
                    ),
                    TestUpsertItem(
                        testId = "45-9",
                        category = "E2E",
                        shortTitle = "Smoke отчет 2",
                        scenario = "Короткий сценарий 2",
                        readyDate = today.toString(),
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runIndex = 1,
                        runStatus = "FAILED",
                        runDate = today.toString(),
                    ),
                ),
            )
        }

        step("Отправляем batch на обновление тестов") {
            reportService.sendBatch(batchRequest)
        }

        val report = step("Запрашиваем отчет о тестах") {
            reportService.getReport()
        }

        step("Проверяем количество записей за выбранную дату") {
            report.items.filter { it.readyDate == today }.shouldHaveSize(2)
        }

        val itemsById = step("Группируем записи отчета по testId") {
            report.items.associateBy { it.testId }
        }

        step("Проверяем первую запись") {
            val firstItem = itemsById["45-7"].shouldNotBeNull()
            firstItem.category shouldBe "E2E"
            firstItem.shortTitle shouldBe "Smoke отчет 1"
            firstItem.readyDate shouldBe today
            firstItem.generalStatus shouldBe GeneralTestStatus.QUEUE.value
            firstItem.runStatuses.first() shouldBe "PASSED"
            firstItem.updatedAt.shouldNotBeNull()
        }

        step("Проверяем вторую запись") {
            val secondItem = itemsById["45-9"].shouldNotBeNull()
            secondItem.category shouldBe "E2E"
            secondItem.shortTitle shouldBe "Smoke отчет 2"
            secondItem.readyDate shouldBe today
            secondItem.generalStatus shouldBe GeneralTestStatus.QUEUE.value
            secondItem.runStatuses.first() shouldBe "FAILED"
            secondItem.updatedAt.shouldNotBeNull()
        }

        step("Проверяем дату первого прогона") {
            val firstRun = report.runs.first { it.runIndex == 1 }
            firstRun.runDate shouldBe today
        }
    }
}
