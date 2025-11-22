package com.example.report.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

data class TestBatchRequest(
    @field:NotEmpty
    val items: List<TestUpsertItem>
)

data class TestUpsertItem(
    val testId: String? = null,
    val category: String? = null,
    val shortTitle: String? = null,
    val issueLink: String? = null,
    val readyDate: String? = null,
    val generalStatus: String? = null,
    val scenario: String? = null,
    val notes: String? = null,
    @field:Min(1)
    @field:Max(5)
    val runIndex: Int? = null,
    val runStatus: String? = null,
    val runDate: String? = null
)
