package org.golenev.tests.backend

import io.kotest.matchers.shouldNotBe
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.endpoints.*
import org.golenev.utils.shouldBe
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("API: Автоматическое определение и появление даты готовности для нового теста")
class ReadyDateAutoDefinitionApiTest {

    private val reportService = ReportServiceDao()

    @AfterEach
    fun cleaDb() {
        TestReportDao.deleteByTestId("123")
    }

    @AllureId("167")
    @Test
    @DisplayName("Готовая дата выставляется автоматически и не изменяется после обновления")
    fun readyDateIsAutoAssignedAndImmutable() {
        val today = step("Определяем дату запуска теста") { LocalDate.now() }

        step("Удаляем отчеты за выбранную дату") {
            TestReportDao.deleteReportsByDate(today)
        }

        val creationRequest = step("Готовим тест-кейс без даты готовности") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = "123",
                        category = "E2E_FOR_AUTOTEST",
                        shortTitle = "Ready date auto set",
                        scenario = ScenarioRequest(
                            steps = listOf(
                                ScenarioStepRequest(
                                    number = 1,
                                    text = "Подготовить batch-запрос для создания нового тест-кейса без readyDate",
                                    attachments = listOf(
                                        ScenarioAttachmentRequest(
                                            name = "text",
                                            content = "POST /api/tests/batch; readyDate intentionally omitted",
                                        ),
                                    ),
                                ),
                                ScenarioStepRequest(
                                    number = 2,
                                    text = "Отправить batch-запрос на создание тест-кейса",
                                    attachments = emptyList(),
                                ),
                                ScenarioStepRequest(
                                    number = 3,
                                    text = "Проверить, что readyDate автоматически выставлен текущей датой",
                                    attachments = emptyList(),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        step("Отправляем запрос на добавление подготовленного тест-кейса") {
            reportService.sendBatch(creationRequest)
        }

        val createdItem = step("Получаем созданную запись и проверяем дату готовности") {
            val report = reportService.getReport()
            report.items.first { it.testId == "123" }
        }

        step("Проверяем, что дата готовности установлена на сегодня") {
            createdItem.readyDate.shouldBe(today, "createdItem.readyDate не совпало с ожидаемым")
        }

        val updateRequest =
            step("Готовим изменения существующего тест-кейса с новой датой готовности") {
                TestBatchRequest(
                    items = listOf(
                        TestUpsertItem(
                            shortTitle = "Ready date auto set",
                            testId = "123",
                            readyDate = today.minusDays(5).toString(),
                            scenario = ScenarioRequest(steps = listOf(ScenarioStepRequest(number = 1, text = "Сценарий 782. шаг 81 шаг 2 шаг 38", attachments = emptyList()))),
                            category = "E2E_FOR_AUTOTEST",
                        ),
                    ),
                )
            }

        step("Отправляем запрос на изменение подготовленного тест-кейса") {
            reportService.sendBatch(updateRequest)
        }

        val updatedItem = step("Получаем обновленную запись") {
            val report = reportService.getReport()
            report.items.first { it.testId == "123" }
        }

        step("Проверяем, что дата готовности сохранилась, а остальные данные изменились") {
            updatedItem.readyDate.shouldBe(createdItem.readyDate, "updatedItem.readyDate не совпало с ожидаемым")
            updatedItem.scenario shouldNotBe createdItem.scenario
            updatedItem.generalStatus.shouldBe(GeneralTestStatus.DONE.value, "updatedItem.generalStatus не совпало с ожидаемым")
        }
    }
}
