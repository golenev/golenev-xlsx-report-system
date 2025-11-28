package com.example.e2e.tests.backend

import com.example.e2e.db.DatabaseCleaner
import com.example.e2e.db.TestReportTable
import com.example.e2e.db.dbReportExec
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.utils.step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestReportE2ETest {

    private val reportService = ReportService()

    @AfterEach
    fun cleaDb () {
        dbReportExec {
            TestReportTable.deleteWhere {
                (testId inList listOf("4856", "11123"))
            }
        }
    }

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
                        testId = "4856",
                        category = "E2E_FOR_AUTOTEST",
                        shortTitle = "Smoke отчет 1",
                        scenario = "Короткий сценарий 1",
                        readyDate = today.toString(),
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runIndex = 1,
                        runStatus = "PASSED",
                        runDate = today.toString(),
                    ),
                    TestUpsertItem(
                        testId = "11123",
                        category = "E2E_FOR_AUTOTEST",
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
            report.items?.filter { it.readyDate == today }?.shouldHaveSize(2) ?: error("записей за выбранную дату ytn")
        }

        val itemsById = step("Группируем записи отчета по testId") {
            report.items?.associateBy { it.testId } ?: error("записи отчета по testId не найдены")
        }

        step("Проверяем первую запись") {
            val firstItem = itemsById["4856"].shouldNotBeNull()
            firstItem.category shouldBe "E2E_FOR_AUTOTEST"
            firstItem.shortTitle shouldBe "Smoke отчет 1"
            firstItem.readyDate shouldBe today
            firstItem.generalStatus shouldBe GeneralTestStatus.QUEUE.value
            firstItem.updatedAt.shouldNotBeNull()
        }

        step("Проверяем вторую запись") {
            val secondItem = itemsById["11123"].shouldNotBeNull()
            secondItem.category shouldBe "E2E_FOR_AUTOTEST"
            secondItem.shortTitle shouldBe "Smoke отчет 2"
            secondItem.readyDate shouldBe today
            secondItem.generalStatus shouldBe GeneralTestStatus.QUEUE.value
            secondItem.updatedAt.shouldNotBeNull()
        }

    }
}
