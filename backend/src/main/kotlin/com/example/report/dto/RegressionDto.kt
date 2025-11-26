package com.example.report.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode

data class RegressionRecordDto(
    val id: Long,
    val regressionDate: String,
    val payload: JsonNode
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RegressionDataDto(
    val status: String?,
    val completedAt: String?
)

enum class RegressionState {
    ACTIVE,
    IDLE
}

data class RegressionStateDto(
    val state: RegressionState,
    val lastCompletedAt: String?
)
