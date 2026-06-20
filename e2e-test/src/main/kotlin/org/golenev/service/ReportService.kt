package org.golenev.service

import io.restassured.response.Response
import org.golenev.dto.TestBatchRequest
import org.golenev.dto.TestReportResponse
import org.golenev.http.Paths
import org.golenev.http.RequestExecutor

class ReportService : RequestExecutor<Unit>(
    path = Paths.REPORTS.path,
) {

    //Отправляем batch запрос для обновления тестов
    fun sendBatch(request: TestBatchRequest, expectedStatus: Int = 200): Response {
        return postRequest(
            url = Paths.REPORTS_BATCH.path,
            requestSpecification = baseRequest().body(request),
            expectedStatus = expectedStatus,
        )
    }

    //Отправляем batch запрос для обновления тестов, в т.ч. неизменяемых полей
    fun sendForceBatch(request: TestBatchRequest, expectedStatus: Int = 200): Response {
        return postRequest(
            url = Paths.REPORTS_BATCH.path,
            requestSpecification = baseRequest().queryParam("forceUpdate", true).body(request),
            expectedStatus = expectedStatus,
        )
    }

    //Читаем отчет о тестах
    fun getReport(): TestReportResponse {
        return getRequest(
            url = path,
            requestSpecification = baseRequest(),
        ).`as`(TestReportResponse::class.java)
    }

    //Сбрасываем данные Run-колонок через API
    fun resetRuns(expectedStatus: Int = 200): Response {
        return postRequest(
            url = Paths.RUNS_RESET.path,
            requestSpecification = baseRequest(),
            expectedStatus = expectedStatus,
        )
    }
}
