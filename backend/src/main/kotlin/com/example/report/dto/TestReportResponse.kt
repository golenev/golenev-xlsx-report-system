package com.example.report.dto

import java.time.LocalDate

data class TestReportResponse(
    val items: List<TestReportItemDto>,
    val runs: List<TestRunMetaDto>,
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
    val runStatuses: List<String?>,
    val updatedAt: String?
)

data class TestRunMetaDto(
    val runIndex: Int,
    val runDate: LocalDate?
)


data class PartialUpdateRequest(
    val category: String? = null,
    val shortTitle: String? = null,
    val issueLink: String? = null,
    val readyDate: String? = null,
    val generalStatus: String? = null,
    val scenario: String? = null,
    val notes: String? = null,
    val runIndex: Int? = null,
    val runStatus: String? = null,
    val runDate: String? = null
)
