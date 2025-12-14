package com.example.e2e.service

import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestReportResponse
import com.example.e2e.http.Paths
import com.example.e2e.http.RequestExecutor
import io.qameta.allure.Step
import io.restassured.response.Response

class ReportService : RequestExecutor<Unit>(
    path = Paths.REPORTS.path,
) {

    @Step("Отправляем batch запрос для обновления тестов")
    fun sendBatch(request: TestBatchRequest, expectedStatus: Int = 200): Response {
        return postRequest(
            url = Paths.REPORTS_BATCH.path,
            requestSpecification = baseRequest().body(request),
            expectedStatus = expectedStatus,
        )
    }

    @Step("Отправляем batch запрос для обновления тестов, в т.ч. неизменяемых полей")
    fun sendForceBatch(request: TestBatchRequest, expectedStatus: Int = 200): Response {
        return postRequest(
            url = Paths.REPORTS_BATCH.path,
            requestSpecification = baseRequest().queryParam("forceUpdate", true).body(request),
            expectedStatus = expectedStatus,
        )
    }

    @Step("Читаем отчет о тестах")
    fun getReport(): TestReportResponse {
        return getRequest(
            url = path,
            requestSpecification = baseRequest(),
        ).`as`(TestReportResponse::class.java)
    }

    @Step("Сбрасываем данные Run-колонок через API")
    fun resetRuns(expectedStatus: Int = 200): Response {
        return postRequest(
            url = Paths.RUNS_RESET.path,
            requestSpecification = baseRequest(),
            expectedStatus = expectedStatus,
        )
    }
}
