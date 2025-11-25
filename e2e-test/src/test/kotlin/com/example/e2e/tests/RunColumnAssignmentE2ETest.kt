package com.example.e2e.tests

import com.example.e2e.db.DatabaseCleaner
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.utils.step
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RunColumnAssignmentE2ETest {

    private val reportService = ReportService()

    @Test
    @DisplayName("Батчи с разными датами попадают в правильные Run-колонки")
    fun batchesAreAssignedToProperRunColumns() {
        val today = step("Определяем сегодняшнюю дату") { LocalDate.now() }
        val tomorrow = step("Определяем завтрашнюю дату") { today.plusDays(1) }

        step("Очищаем данные по нужным датам и сбрасываем Run-колонки") {
            DatabaseCleaner.deleteReportsByDate(today)
            DatabaseCleaner.deleteReportsByDate(tomorrow)
            reportService.resetRuns()
        }

        val emptyReport = step("Проверяем, что заголовки Run очищены") {
            reportService.getReport()
        }
        step("Все Run-колонки не имеют даты") {
            emptyReport.runs.map { it.runDate } shouldContainExactly listOf(null, null, null, null, null)
        }

        val firstBatch = step("Формируем первый батч с сегодняшней датой") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "RUN-DATE-1",
                        category = "E2E",
                        shortTitle = "Сегодня PASSED",
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runStatus = "PASSED",
                        runDate = today.toString(),
                        readyDate = today.toString(),
                    ),
                    TestUpsertItem(
                        testId = "RUN-DATE-2",
                        category = "E2E",
                        shortTitle = "Сегодня FAILED",
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runStatus = "FAILED",
                        runDate = today.toString(),
                        readyDate = today.toString(),
                    ),
                ),
            )
        }

        step("Отправляем первый батч") {
            reportService.sendBatch(firstBatch)
        }

        val afterFirstBatch = step("Читаем отчет после первого батча") {
            reportService.getReport()
        }

        step("Первая колонка Run выставлена на сегодня, остальные пустые") {
            val runDates = afterFirstBatch.runs.associateBy { it.runIndex }
            runDates[1]?.runDate shouldBe today
            runDates[2]?.runDate.shouldBeNull()
        }

        step("Статусы тестов записаны в первую колонку") {
            val items = afterFirstBatch.items.associateBy { it.testId }
            items["RUN-DATE-1"].shouldNotBeNull().runStatuses[0] shouldBe "PASSED"
            items["RUN-DATE-1"].shouldNotBeNull().runStatuses[1].shouldBeNull()
            items["RUN-DATE-2"].shouldNotBeNull().runStatuses[0] shouldBe "FAILED"
            items["RUN-DATE-2"].shouldNotBeNull().runStatuses[1].shouldBeNull()
        }

        val secondBatch = step("Формируем второй батч с завтрашней датой") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "RUN-DATE-3",
                        category = "E2E",
                        shortTitle = "Завтра PASSED",
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runStatus = "PASSED",
                        runDate = tomorrow.toString(),
                        readyDate = tomorrow.toString(),
                    ),
                    TestUpsertItem(
                        testId = "RUN-DATE-4",
                        category = "E2E",
                        shortTitle = "Завтра FAILED",
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runStatus = "FAILED",
                        runDate = tomorrow.toString(),
                        readyDate = tomorrow.toString(),
                    ),
                ),
            )
        }

        step("Отправляем второй батч") {
            reportService.sendBatch(secondBatch)
        }

        val afterSecondBatch = step("Читаем отчет после второго батча") {
            reportService.getReport()
        }

        step("Дата сегодняшнего запуска сохранилась в первой колонке, завтрашняя попала во вторую") {
            val runDates = afterSecondBatch.runs.associateBy { it.runIndex }
            runDates[1]?.runDate shouldBe today
            runDates[2]?.runDate shouldBe tomorrow
        }

        step("Статусы второго батча записаны во вторую колонку") {
            val items = afterSecondBatch.items.associateBy { it.testId }
            items["RUN-DATE-3"].shouldNotBeNull().runStatuses[1] shouldBe "PASSED"
            items["RUN-DATE-3"].shouldNotBeNull().runStatuses[0].shouldBeNull()
            items["RUN-DATE-4"].shouldNotBeNull().runStatuses[1] shouldBe "FAILED"
            items["RUN-DATE-4"].shouldNotBeNull().runStatuses[0].shouldBeNull()
        }
    }
}
