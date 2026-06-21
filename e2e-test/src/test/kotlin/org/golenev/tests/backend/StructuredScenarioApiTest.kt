package org.golenev.tests.backend

import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.endpoints.ReportServiceDao
import org.golenev.restapi.endpoints.ScenarioAttachmentRequest
import org.golenev.restapi.endpoints.ScenarioRequest
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.restapi.endpoints.TestUpsertItem
import org.golenev.restapi.endpoints.ErrorResponse
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("API: structured scenario для тест-кейсов")
class StructuredScenarioApiTest {

    private val reportService = ReportServiceDao()
    private val createdTestIds = mutableListOf<String>()

    @AfterEach
    fun cleanDb() {
        createdTestIds.forEach { testId -> TestReportDao.deleteByTestId(testId) }
        createdTestIds.clear()
    }

    @AllureId("230")
    @Test
    @DisplayName("Создаём тест-кейс со structured scenario без поля format")
    fun createTestWithStructuredScenarioWithoutFormat() {
        val testId = nextTestId()
        val scenario = ScenarioRequest(
            steps = listOf(
                ScenarioStepRequest(number = 1, text = "Первый шаг", attachments = emptyList()),
                ScenarioStepRequest(number = 2, text = "Второй шаг", attachments = emptyList()),
            ),
        )

        step("Создаём тест-кейс со structured scenario") {
            reportService.sendTest(validItem(testId, scenario))
        }

        val actualScenario = step("Получаем созданный structured scenario") {
            reportService.getReport().items.first { it.testId == testId }.scenario
        }

        step("Проверяем, что шаги вернулись объектами без format") {
            val steps = actualScenario?.steps
            steps?.size shouldBe 2
            steps?.get(0)?.number shouldBe 1
            steps?.get(0)?.text shouldBe "Первый шаг"
            steps?.get(0)?.attachments shouldBe emptyList()
            steps?.get(1)?.number shouldBe 2
            steps?.get(1)?.text shouldBe "Второй шаг"
        }
    }

    @AllureId("231")
    @Test
    @DisplayName("Обновляем тест-кейс со structured scenario и вложением у шага")
    fun updateTestWithStructuredScenarioAttachment() {
        val testId = nextTestId()
        step("Создаём тест-кейс со structured scenario") {
            reportService.sendTest(
                validItem(
                    testId,
                    ScenarioRequest(
                        steps = listOf(
                            ScenarioStepRequest(number = 1, text = "Первый шаг до обновления", attachments = emptyList()),
                        ),
                    ),
                ),
            )
        }

        val scenario = ScenarioRequest(
            steps = listOf(
                ScenarioStepRequest(
                    number = 1,
                    text = "Шаг с вложением",
                    attachments = listOf(
                        ScenarioAttachmentRequest(
                            type = "text",
                            content = "request / response / json / curl",
                        ),
                    ),
                ),
                ScenarioStepRequest(number = 2, text = "Шаг без вложения", attachments = emptyList()),
            ),
        )

        step("Обновляем тест-кейс structured scenario без поля format") {
            reportService.sendTest(validItem(testId, scenario))
        }

        val actualScenario = step("Получаем обновлённый structured scenario") {
            reportService.getReport().items.first { it.testId == testId }.scenario
        }

        step("Проверяем, что вложение осталось у первого шага") {
            val steps = actualScenario?.steps
            steps?.size shouldBe 2
            val firstAttachments = steps?.get(0)?.attachments
            firstAttachments?.size shouldBe 1
            firstAttachments?.get(0)?.type shouldBe "text"
            firstAttachments?.get(0)?.content shouldBe "request / response / json / curl"
            steps?.get(1)?.attachments shouldBe emptyList()
        }
    }

    @AllureId("232")
    @Test
    @DisplayName("Пустые шаги structured scenario не сохраняются")
    fun emptyStructuredScenarioStepsAreIgnored() {
        val testId = nextTestId()
        val scenario = ScenarioRequest(
            steps = listOf(
                ScenarioStepRequest(number = 1, text = "Первый шаг", attachments = emptyList()),
                ScenarioStepRequest(number = 2, text = "   ", attachments = emptyList()),
                ScenarioStepRequest(number = 3, text = "Третий шаг", attachments = emptyList()),
            ),
        )

        step("Создаём тест-кейс со structured scenario и пустым шагом") {
            reportService.sendTest(validItem(testId, scenario))
        }

        step("Проверяем, что пустой шаг не вернулся как полноценный шаг") {
            val steps = reportService.getReport().items.first { it.testId == testId }.scenario?.steps
            steps?.size shouldBe 2
            steps?.get(0)?.number shouldBe 1
            steps?.get(1)?.number shouldBe 3
        }
    }

    @AllureId("233")
    @Test
    @DisplayName("String scenario не принимается API")
    fun stringScenarioIsRejected() {
        val testId = nextTestId()
        val response = step("Отправляем test-case со scenario в виде строки") {
            reportService.sendTestBody(
                request = mapOf(
                    "testId" to testId,
                    "category" to "API",
                    "shortTitle" to "Structured scenario only",
                    "scenario" to "1. Больше не поддерживаем string scenario",
                ),
                expectedStatus = 400,
            )
        }

        val errorResponse = response.`as`(ErrorResponse::class.java)

        step("Проверяем, что API требует scenario object") {
            response.statusCode shouldBe 400
            errorResponse.message shouldBe "Field scenario must be an object"
        }
    }

    private fun nextTestId(): String {
        val testId = "STRUCT-${getRandomTestId()}"
        createdTestIds.add(testId)
        return testId
    }

    private fun validItem(testId: String, scenario: ScenarioRequest?) = TestUpsertItem(
        testId = testId,
        category = "API",
        shortTitle = "Structured scenario",
        scenario = scenario,
    )
}
