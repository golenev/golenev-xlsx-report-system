package com.example.report.dto

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

data class TestReportResponse(
    val items: List<TestReportItemDto>,
    val columnConfig: Map<String, Int>,
    val translations: Map<String, String> = emptyMap()
)

data class TestReportItemDto(
    val testId: String,
    val category: String?,
    val shortTitle: String?,
    val issueLink: String?,
    val readyDate: LocalDate?,
    val generalStatus: String?,
    val priority: String?,
    val scenario: JsonNode?,
    val notes: String?,
    val updatedAt: String?,
    val runStatus: String?,
)
