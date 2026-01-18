package com.example.e2e.tests.backend

import com.example.e2e.db.repository.TestReportRepository
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.utils.step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.qameta.allure.AllureId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("API: Автоматическое определение и появление даты готовности для нового теста")
class ReadyDateAutoDefinitionApiTest {

    private val reportService = ReportService()

    @AfterEach
    fun cleaDb() {
        TestReportRepository.deleteByTestId("123")
    }

    @AllureId("167")
    @Test
    @DisplayName("Готовая дата выставляется автоматически и не изменяется после обновления")
    fun readyDateIsAutoAssignedAndImmutable() {
        val today = step("Определяем дату запуска теста") { LocalDate.now() }

        step("Удаляем отчеты за выбранную дату") {
            TestReportRepository.deleteReportsByDate(today)
        }

        val creationRequest = step("Формируем batch-запрос без readyDate") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "123",
                        category = "E2E_FOR_AUTOTEST",
                        shortTitle = "Ready date auto set",
                        scenario = "Сценарий 1. шаг 1 шаг 2 шаг 3",
                    ),
                ),
            )
        }

        step("Отправляем batch на создание записи") {
            reportService.sendBatch(creationRequest)
        }

        val createdItem = step("Получаем созданную запись и проверяем readyDate") {
            val report = reportService.getReport()
            report.items.first { it.testId == "123" }
        }

        step("Готовая дата установлена на сегодняшнее число") {
            createdItem.readyDate shouldBe today
        }

        val updateRequest = step("Формируем batch-запрос с попыткой изменить readyDate для уже имеющейся записи") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        shortTitle = "Ready date auto set",
                        testId = "123",
                        readyDate = today.minusDays(5).toString(),
                        scenario = "Сценарий 782. шаг 81 шаг 2 шаг 38",
                        category = "E2E_FOR_AUTOTEST",
                    ),
                ),
            )
        }

        step("Отправляем batch на обновление записи") {
            reportService.sendBatch(updateRequest)
        }

        val updatedItem = step("Получаем обновленную запись") {
            val report = reportService.getReport()
            report.items.first { it.testId == "123" }
        }

        step("Готовая дата осталась прежней, остальные поля обновлены") {
            updatedItem.readyDate shouldBe createdItem.readyDate
            updatedItem.scenario shouldNotBe createdItem.scenario
            updatedItem.generalStatus shouldBe GeneralTestStatus.DONE.value
        }
    }
}
