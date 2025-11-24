package com.example.e2e.tests

import com.example.e2e.db.DatabaseCleaner
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.utils.step
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DuplicateTestIdE2ETest {

    private val reportService = ReportService()

    @Test
    @DisplayName("Ошибка при создании теста с уже существующим testId")
    fun failToCreateDuplicateTestId() {
        val readyDate = step("Определяем дату для теста") { LocalDate.now() }

        step("Удаляем отчеты за выбранную дату") {
            DatabaseCleaner.deleteReportsByDate(readyDate)
        }

        val initialRequest = step("Формируем batch-запрос для первоначального создания записи") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "DUPLICATE-CREATE-1",
                        category = "E2E",
                        shortTitle = "Исходная запись",
                        readyDate = readyDate.toString(),
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runIndex = 1,
                        runStatus = "PASSED",
                        runDate = readyDate.toString(),
                    ),
                ),
            )
        }

        step("Отправляем batch на создание исходной записи") {
            reportService.sendBatch(initialRequest).statusCode shouldBe 200
        }

        val duplicateRequest = step("Формируем batch-запрос с дублирующимся testId") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "DUPLICATE-CREATE-1",
                        category = "API",
                        shortTitle = "Дубликат записи",
                        readyDate = readyDate.plusDays(1).toString(),
                        generalStatus = GeneralTestStatus.DONE.value,
                        runIndex = 2,
                        runStatus = "FAILED",
                        runDate = readyDate.plusDays(1).toString(),
                    ),
                ),
            )
        }

        step("Получаем ошибку при попытке создания записи с уже существующим testId") {
            reportService.sendBatch(duplicateRequest, expectedStatus = 400).statusCode shouldBe 400
        }

        val report = step("Получаем отчет после неуспешной попытки") {
            reportService.getReport()
        }

        val createdItem = step("Находим исходную запись в отчете") {
            report.items.first { it.testId == "DUPLICATE-CREATE-1" }
        }

        step("Проверяем, что исходная запись не была перезаписана") {
            createdItem.readyDate shouldBe readyDate
            createdItem.category shouldBe "E2E"
            createdItem.shortTitle shouldBe "Исходная запись"
            createdItem.generalStatus shouldBe GeneralTestStatus.QUEUE.value
            createdItem.runStatuses.filterNotNull() shouldContainExactly listOf("PASSED")
        }
    }

    @Test
    @DisplayName("Ошибка при обновлении теста с testId, принадлежащим другой записи")
    fun failToUpdateTestWithDuplicateTestId() {
        val readyDate = step("Определяем дату для тестов") { LocalDate.now() }

        step("Удаляем отчеты за выбранную дату") {
            DatabaseCleaner.deleteReportsByDate(readyDate)
        }

        val setupRequest = step("Создаем две независимые записи") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "DUPLICATE-EDIT-ORIGINAL",
                        category = "API",
                        shortTitle = "Оригинальная запись",
                        readyDate = readyDate.toString(),
                        generalStatus = GeneralTestStatus.IN_PROGRESS.value,
                        runIndex = 1,
                        runStatus = "PASSED",
                        runDate = readyDate.toString(),
                    ),
                    TestUpsertItem(
                        testId = "DUPLICATE-EDIT-TARGET",
                        category = "E2E",
                        shortTitle = "Запись для редактирования",
                        readyDate = readyDate.toString(),
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        runIndex = 1,
                        runStatus = "FAILED",
                        runDate = readyDate.toString(),
                    ),
                ),
            )
        }

        step("Отправляем batch на создание двух записей") {
            reportService.sendBatch(setupRequest).statusCode shouldBe 200
        }

        val conflictingUpdate = step("Формируем запрос с testId, который уже принадлежит другой записи") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "DUPLICATE-EDIT-ORIGINAL",
                        shortTitle = "Обновление другой записи",
                        generalStatus = GeneralTestStatus.DONE.value,
                        runIndex = 2,
                        runStatus = "FAILED",
                        runDate = readyDate.plusDays(1).toString(),
                    ),
                ),
            )
        }

        step("Получаем ошибку при попытке обновления с дублирующим testId") {
            reportService.sendBatch(conflictingUpdate, expectedStatus = 400).statusCode shouldBe 400
        }

        val report = step("Запрашиваем отчет для проверки текущего состояния записей") {
            reportService.getReport()
        }

        val itemsById = step("Группируем записи по testId") {
            report.items.associateBy { it.testId }
        }

        step("Проверяем, что исходная запись осталась без изменений") {
            val original = itemsById["DUPLICATE-EDIT-ORIGINAL"]
            original shouldNotBe null
            original!!.shortTitle shouldBe "Оригинальная запись"
            original.category shouldBe "API"
            original.generalStatus shouldBe GeneralTestStatus.IN_PROGRESS.value
            original.runStatuses.filterNotNull() shouldContainExactly listOf("PASSED")
        }

        step("Проверяем, что запись для редактирования не была изменена") {
            val target = itemsById["DUPLICATE-EDIT-TARGET"]
            target shouldNotBe null
            target!!.shortTitle shouldBe "Запись для редактирования"
            target.category shouldBe "E2E"
            target.generalStatus shouldBe GeneralTestStatus.QUEUE.value
            target.runStatuses.filterNotNull() shouldHaveSize 1
            target.runStatuses.first() shouldBe "FAILED"
        }
    }
}
