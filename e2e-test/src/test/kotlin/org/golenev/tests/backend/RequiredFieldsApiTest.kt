package org.golenev.tests.backend

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.qameta.allure.AllureId
import org.golenev.restapi.endpoints.ErrorResponse
import org.golenev.restapi.endpoints.ReportServiceDao
import org.golenev.restapi.endpoints.TestBatchRequest
import org.golenev.restapi.endpoints.TestUpsertItem
import org.golenev.utils.step
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@DisplayName("Проверка обязательности полей при добавлении нового тест кейса")
class RequiredFieldsApiTest {

    private val reportService = ReportServiceDao()

    @AllureId("168")
    @ParameterizedTest
    @MethodSource("missingFieldProvider")
    @DisplayName("Отсутствующие обязательные поля приводят к 400")
    fun requiredFieldIsValidated(field: String, expectedMessage: String, payload: TestUpsertItem) {
        val response = step("Отправляем batch без обязательного поля $field") {
            reportService.sendBatch(
                request = TestBatchRequest(items = listOf(payload)),
                expectedStatus = 400,
            )
        }

        val errorResponse = response.`as`(ErrorResponse::class.java)

        step("Проверяем статус код и упоминание для отсутствующего поля $field") {
            response.statusCode shouldBe 400
            val actualMessage = errorResponse.message.shouldNotBeNull()
            actualMessage shouldContain expectedMessage
            errorResponse.missingField shouldBe field
        }
    }

    companion object {
        @JvmStatic
        fun missingFieldProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "testId",
                "Required field testId is missing",
                validItem().copy(testId = null)
            ),
            Arguments.of(
                "category",
                "Required field category is missing",
                validItem().copy(category = null)
            ),
            Arguments.of(
                "shortTitle",
                "Required field shortTitle is missing",
                validItem().copy(shortTitle = null)
            ),
            Arguments.of(
                "scenario",
                "Required field scenario is missing",
                validItem().copy(scenario = null)
            ),
        )

        private fun validItem() = TestUpsertItem(
            testId = "REQ-1",
            category = "E2E",
            shortTitle = "Проверка обязательных полей",
            scenario = "Отправляем запрос с пропущенными полями",
        )
    }
}
