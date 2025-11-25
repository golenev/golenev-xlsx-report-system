package com.example.e2e.tests

import com.example.e2e.dto.ErrorResponse
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.utils.step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class RequiredFieldsE2ETest {

    private val reportService = ReportService()

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
                "generalStatus",
                "Required field generalStatus is missing",
                validItem().copy(generalStatus = null)
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
            generalStatus = GeneralTestStatus.QUEUE.value,
        )
    }
}

