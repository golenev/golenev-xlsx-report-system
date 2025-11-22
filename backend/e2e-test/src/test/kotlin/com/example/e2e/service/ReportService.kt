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
    fun sendBatch(request: TestBatchRequest): Response {
        return postRequest(
            url = Paths.REPORTS_BATCH.path,
            requestSpecification = baseRequest().body(request),
        )
    }

    @Step("Читаем отчет о тестах")
    fun getReport(): TestReportResponse {
        return getRequest(
            url = path,
            requestSpecification = baseRequest(),
        ).`as`(TestReportResponse::class.java)
    }
}
