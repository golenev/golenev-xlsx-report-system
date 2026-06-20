package org.golenev.restapi.endpoints

import io.restassured.response.Response
import org.golenev.restapi.config.Paths
import org.golenev.restapi.config.RequestExecutor

class ReportServiceDao : RequestExecutor<Unit>(
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
