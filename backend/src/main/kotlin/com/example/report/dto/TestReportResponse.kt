package com.example.report.dto

import java.time.LocalDate

data class TestReportResponse(
    val items: List<TestReportItemDto>,
    val columnConfig: Map<String, Int>
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
    val updatedAt: String?
)
