package com.example.report.dto

import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotNull

data class RegressionRequest(
    @field:NotNull
    val regressionDate: String?,
    @field:NotNull
    val payload: JsonNode?
)

data class RegressionRecordDto(
    val id: Long,
    val regressionDate: String,
    val payload: JsonNode
)
