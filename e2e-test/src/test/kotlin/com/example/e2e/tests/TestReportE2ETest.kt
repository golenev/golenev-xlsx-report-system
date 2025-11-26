package com.example.e2e.tests

import com.example.e2e.db.DatabaseCleaner
import com.example.e2e.db.RegressionTable
import com.example.e2e.db.TestReportTable
import com.example.e2e.db.dbReportExec
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.dto.RegressionState
import com.example.e2e.service.ReportService
import com.example.e2e.utils.step
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestReportE2ETest {

    private val reportService = ReportService()

    @AfterEach
    fun cleanDb() {
        dbReportExec {
            TestReportTable.deleteAll()
            RegressionTable.deleteAll()
        }
    }

    @Test
    @DisplayName("Регресс запускается, завершается и сохраняет jsonb-запись")
    fun regressionLifecycleCreatesHistoryRecord() {
        val regressionDate = step("Выбираем дату регресса") { LocalDate.now() }

        step("Очищаем данные за выбранную дату") {
            DatabaseCleaner.deleteReportsByDate(regressionDate)
            DatabaseCleaner.deleteRegressionByDate(regressionDate)
        }

        val batchRequest = step("Формируем batch-запрос с тестами регресса") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "4856",
                        category = "E2E_FOR_AUTOTEST",
                        shortTitle = "Smoke отчет 1",
                        scenario = "Короткий сценарий 1",
                        readyDate = regressionDate.toString(),
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        regressionStatus = "FAILED",
                        regressionDate = regressionDate.toString(),
                    ),
                    TestUpsertItem(
                        testId = "11123",
                        category = "E2E_FOR_AUTOTEST",
                        shortTitle = "Smoke отчет 2",
                        scenario = "Короткий сценарий 2",
                        readyDate = regressionDate.toString(),
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        regressionStatus = "PASSED",
                        regressionDate = regressionDate.toString(),
                    ),
                ),
            )
        }

        step("Отправляем batch на обновление тестов") {
            reportService.sendBatch(batchRequest)
        }

        val reportBeforeCompletion = step("Запрашиваем отчет о тестах до завершения регресса") {
            reportService.getReport()
        }

        step("Проверяем состояние регресса и записи") {
            reportBeforeCompletion.regression.state shouldBe RegressionState.ACTIVE
            val itemsById = reportBeforeCompletion.items.associateBy { it.testId }
            itemsById.keys.shouldContainExactlyInAnyOrder("4856", "11123")
            itemsById.values.forEach { item ->
                item.regressionStatus.shouldNotBeNull()
                item.regressionDate shouldBe regressionDate
                item.regression.shouldNotBeNull()
            }
        }

        step("Завершаем регресс через API") {
            reportService.completeRegression()
        }

        val reportAfterCompletion = step("Повторно читаем отчет") { reportService.getReport() }

        step("Регресс завершен, записи очищены, но дата завершения проставлена") {
            reportAfterCompletion.regression.state shouldBe RegressionState.IDLE
            reportAfterCompletion.regression.lastCompletedAt shouldBe regressionDate.toString()

            reportAfterCompletion.items.forEach { item ->
                item.regressionStatus.shouldBeNull()
                item.regressionDate.shouldBeNull()
                item.regression.shouldBeNull()
            }
        }

        step("Проверяем сохраненный jsonb-пейлоад регресса") {
            val savedPayload = dbReportExec {
                RegressionTable.selectAll().single()[RegressionTable.payload]
            }
            val payloadNode = jacksonObjectMapper().readTree(savedPayload)
            payloadNode["items"].size() shouldBe 2
            payloadNode["items"][0]["testId"].asText() shouldBe "4856"
            payloadNode["items"][1]["testId"].asText() shouldBe "11123"
        }
    }
}
