package com.example.e2e.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TestBatchRequest(
    val items: List<TestUpsertItem>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TestUpsertItem(
    val testId: String? = null,
    val category: String? = null,
    val shortTitle: String? = null,
    val issueLink: String? = null,
    val readyDate: String? = null,
    val generalStatus: String? = null,
    val scenario: String? = null,
    val notes: String? = null,
    val regressionStatus: String? = null,
    val regressionDate: String? = null,
    val regression: RegressionDataPayload? = null,
)

data class TestReportResponse(
    val items: List<TestReportItemDto>,
    val columnConfig: Map<String, Int>,
    val regression: RegressionStateDto,
)

data class TestReportItemDto(
    val testId: String,
    val category: String?,
    val shortTitle: String?,
    val issueLink: String?,
    val readyDate: LocalDate?,
    val generalStatus: String?,
    val scenario: String?,
    val notes: String?,
    val regressionStatus: String?,
    val regressionDate: LocalDate?,
    val regression: RegressionDataDto?,
    val updatedAt: String?,
)

data class RegressionDataPayload(
    val status: String? = null,
    val completedAt: String? = null,
)

data class RegressionDataDto(
    val status: String?,
    val completedAt: String?,
)

enum class RegressionState { ACTIVE, IDLE }

data class RegressionStateDto(
    val state: RegressionState,
    val lastCompletedAt: String?,
)

data class ErrorResponse(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: Any? = null,
    val message: String? = null,
    val path: String? = null,
    val missingField: String? = null,
    val items: List<TestUpsertItem>? = null,
)

enum class GeneralTestStatus(val value: String) {
    QUEUE("Очередь"),
    IN_PROGRESS("В работе"),
    DONE("Готово"),
    BACKLOG("Бэклог"),
    MANUAL_ONLY("Только ручное"),
    OUTDATED("Неактуально"),
    FRONT("Фронт"),
}
