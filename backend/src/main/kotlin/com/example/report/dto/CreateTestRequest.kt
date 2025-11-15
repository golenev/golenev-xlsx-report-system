package com.example.report.dto

import jakarta.validation.constraints.NotBlank

data class CreateTestRequest(
    @field:NotBlank
    val testId: String,
    val category: String? = null,
    val shortTitle: String? = null,
    val issueLink: String? = null,
    val readyDate: String? = null,
    val generalStatus: String? = null,
    val scenario: String? = null,
    val notes: String? = null
)
