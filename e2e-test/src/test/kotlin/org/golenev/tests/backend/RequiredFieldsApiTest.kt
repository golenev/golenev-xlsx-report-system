package org.golenev.tests.backend

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.qameta.allure.AllureId
import io.restassured.response.Response
import org.golenev.restapi.endpoints.*
import org.golenev.utils.shouldBe
import org.golenev.utils.step
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Проверка обязательности полей при добавлении нового тест кейса")
class RequiredFieldsApiTest {

    private val reportService = ReportServiceDao()

    @Test
    @AllureId("168")
    @DisplayName("Отсутствие обязательного поля testId приводит к 400")
    fun shouldRejectBatchWithoutTestId() {
        val response = missingRequiredFieldTemplate("Идентификатор тест-кейса") {
            it.copy(testId = null)
        }

        step("Проверяем сообщение об отсутствии обязательного поля «Идентификатор тест-кейса»") {
            val errorResponse = response.`as`(ErrorResponse::class.java)
            response.statusCode.shouldBe(400, "response.statusCode не совпало с ожидаемым")
            val actualMessage = errorResponse.message.shouldNotBeNull()
            actualMessage shouldContain "Required field testId is missing"
            errorResponse.missingField.shouldBe("testId", "errorResponse.missingField не совпало с ожидаемым")
        }
    }

    @Test
    @AllureId("328")
    @DisplayName("Отсутствие обязательного поля category приводит к 400")
    fun shouldRejectBatchWithoutCategory() {
        val response = missingRequiredFieldTemplate("Категория") {
            it.copy(category = null)
        }

        step("Проверяем сообщение об отсутствии обязательного поля «Категория»") {
            val errorResponse = response.`as`(ErrorResponse::class.java)
            response.statusCode.shouldBe(400, "response.statusCode не совпало с ожидаемым")
            val actualMessage = errorResponse.message.shouldNotBeNull()
            actualMessage shouldContain "Required field category is missing"
            errorResponse.missingField.shouldBe("category", "errorResponse.missingField не совпало с ожидаемым")
        }
    }

    @Test
    @AllureId("329")
    @DisplayName("Отсутствие обязательного поля shortTitle приводит к 400")
    fun shouldRejectBatchWithoutShortTitle() {
        val response = missingRequiredFieldTemplate("Название") {
            it.copy(shortTitle = null)
        }

        step("Проверяем сообщение об отсутствии обязательного поля «Название»") {
            val errorResponse = response.`as`(ErrorResponse::class.java)
            response.statusCode.shouldBe(400, "response.statusCode не совпало с ожидаемым")
            val actualMessage = errorResponse.message.shouldNotBeNull()
            actualMessage shouldContain "Required field shortTitle is missing"
            errorResponse.missingField.shouldBe("shortTitle", "errorResponse.missingField не совпало с ожидаемым")
        }
    }

    @Test
    @AllureId("330")
    @DisplayName("Отсутствие обязательного поля scenario приводит к 400")
    fun shouldRejectBatchWithoutScenario() {
        val response = missingRequiredFieldTemplate("Сценарий") {
            it.copy(scenario = null)
        }

        step("Проверяем сообщение об отсутствии обязательного поля «Сценарий»") {
            val errorResponse = response.`as`(ErrorResponse::class.java)
            response.statusCode.shouldBe(400, "response.statusCode не совпало с ожидаемым")
            val actualMessage = errorResponse.message.shouldNotBeNull()
            actualMessage shouldContain "Required field scenario is missing"
            errorResponse.missingField.shouldBe("scenario", "errorResponse.missingField не совпало с ожидаемым")
        }
    }

    private fun missingRequiredFieldTemplate(
        field: String,
        omitField: (TestUpsertItem) -> TestUpsertItem,
    ): Response {
        val payload = step("Готовим тест-кейс без обязательного поля «$field»") {
            omitField(
                TestUpsertItem(
                    testId = "REQ-1",
                    category = "E2E",
                    shortTitle = "Проверка обязательных полей",
                    scenario = ScenarioRequest(
                        steps = listOf(
                            ScenarioStepRequest(
                                number = 1,
                                text = "Отправляем запрос с пропущенными полями",
                                attachments = emptyList(),
                            ),
                        ),
                    ),
                ),
            )
        }

        return step("Отправляем запрос на добавление подготовленного тест-кейса без поля «$field»") {
            reportService.sendBatch(
                request = TestBatchRequest(items = listOf(payload)),
                expectedStatus = 400,
            )
        }
    }
}
