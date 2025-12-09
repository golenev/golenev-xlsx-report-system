package com.example.report.dto

import com.fasterxml.jackson.annotation.JsonAlias
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
    val priority: String? = null,
    val scenario: String? = null,
    val notes: String? = null,
    @JsonAlias("run_status")
    val runStatus: String? = null,
)
