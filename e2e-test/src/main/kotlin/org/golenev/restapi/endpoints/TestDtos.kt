package org.golenev.restapi.endpoints

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TestBatchRequest(
    val items: List<TestUpsertItem>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TestUpsertItem(
    val testId: String?,
    val category: String?,
    val shortTitle: String?,
    val issueLink: String? = null,
    val readyDate: String = LocalDate.now().toString(),
    val generalStatus: String? = "Готово",
    val priority: String? = "Medium",
    val scenario: ScenarioRequest?,
    val notes: String? = null,
    val runStatus: String? = null,
    val runDate: String? = null,
)

data class TestReportResponse(
    val items: List<TestReportItemDto>,
    val columnConfig: Map<String, Int>,
    val translations: Map<String, String> = emptyMap(),
)

data class TestReportItemDto(
    val testId: String,
    val category: String?,
    val shortTitle: String?,
    val issueLink: String?,
    val readyDate: LocalDate?,
    val generalStatus: String?,
    val priority: String?,
    val scenario: ScenarioRequest?,
    val notes: String?,
    val updatedAt: String?,
    val runStatus: String? = null
)

data class ScenarioRequest(
    val steps: List<ScenarioStepRequest>,
)

data class ScenarioStepRequest(
    val number: Int,
    val text: String,
    val attachments: List<ScenarioAttachmentRequest>,
)

data class ScenarioAttachmentRequest(
    val type: String,
    val content: String,
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
