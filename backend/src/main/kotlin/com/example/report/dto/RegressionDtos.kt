package com.example.report.dto

import com.example.report.model.RegressionStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

private val allowedRegressionResults = setOf("PASSED", "FAILED", "SKIPPED")

fun validateRegressionResults(results: Map<String, String>): Map<String, String> {
    return results.mapValues { (testId, value) ->
        val normalized = value.trim().uppercase()
        if (normalized.isEmpty()) {
            throw IllegalArgumentException("Regression status for $testId is empty")
        }
        if (normalized !in allowedRegressionResults) {
            throw IllegalArgumentException("Invalid regression status $value for $testId")
        }
        normalized
    }
}

data class RegressionStopRequest(
    @field:NotEmpty
    val results: Map<String, String>,
)

data class RegressionStartRequest(
    @field:NotBlank
    val releaseName: String,
)

data class RegressionStateResponse(
    val status: RegressionStatus,
    val regressionDate: String?,
    val results: Map<String, String> = emptyMap(),
    val releaseName: String? = null,
)

data class RegressionReleaseSummary(
    val id: Long,
    val name: String,
    val regressionDate: String?,
    val status: RegressionStatus,
)

data class RegressionSnapshotResponse(
    val id: Long,
    val name: String,
    val status: RegressionStatus,
    val regressionDate: String?,
    val snapshot: Map<String, Any?>,
)
