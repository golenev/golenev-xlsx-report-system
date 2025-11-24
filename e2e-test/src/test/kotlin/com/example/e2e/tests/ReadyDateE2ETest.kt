package com.example.e2e.tests

import com.example.e2e.db.DatabaseCleaner
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.utils.step
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReadyDateE2ETest {

    private val reportService = ReportService()

    @Test
    @DisplayName("Готовая дата выставляется автоматически и не изменяется после обновления")
    fun readyDateIsAutoAssignedAndImmutable() {
        val today = step("Определяем дату запуска теста") { LocalDate.now() }

        step("Удаляем отчеты за выбранную дату") {
            DatabaseCleaner.deleteReportsByDate(today)
        }

        val creationRequest = step("Формируем batch-запрос без readyDate") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "READY-IMMUTABLE-1",
                        category = "E2E",
                        shortTitle = "Ready date auto set",
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runIndex = 1,
                        runStatus = "PASSED",
                        runDate = today.toString(),
                    ),
                ),
            )
        }

        step("Отправляем batch на создание записи") {
            reportService.sendBatch(creationRequest)
        }

        val createdItem = step("Получаем созданную запись и проверяем readyDate") {
            val report = reportService.getReport()
            report.items.first { it.testId == "READY-IMMUTABLE-1" }
        }

        step("Готовая дата установлена на сегодняшнее число") {
            createdItem.readyDate shouldBe today
        }

        val updateRequest = step("Формируем batch-запрос с попыткой изменить readyDate") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "READY-IMMUTABLE-1",
                        readyDate = "2000-01-01",
                        generalStatus = GeneralTestStatus.DONE.value,
                        notes = "Updated notes",
                    ),
                ),
            )
        }

        step("Отправляем batch на обновление записи") {
            reportService.sendBatch(updateRequest)
        }

        val updatedItem = step("Получаем обновленную запись") {
            val report = reportService.getReport()
            report.items.first { it.testId == "READY-IMMUTABLE-1" }
        }

        step("Готовая дата осталась прежней, остальные поля обновлены") {
            updatedItem.readyDate shouldBe today
            updatedItem.generalStatus shouldBe GeneralTestStatus.DONE.value
            updatedItem.notes shouldBe "Updated notes"
        }
    }
}
