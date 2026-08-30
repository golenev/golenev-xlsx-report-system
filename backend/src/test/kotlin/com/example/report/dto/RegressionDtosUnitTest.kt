package com.example.report.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RegressionDtosUnitTest {

    @Test
    fun `regression results normalize whitespace and case without changing test ids`() {
        val normalized = validateRegressionResults(
            linkedMapOf(
                " T-1 " to " passed ",
                "T-2" to "Failed",
                "T-3" to "SKIPPED",
            ),
        )

        assertEquals(
            linkedMapOf(" T-1 " to "PASSED", "T-2" to "FAILED", "T-3" to "SKIPPED"),
            normalized,
        )
    }

    @Test
    fun `regression results reject empty and unsupported status with exact test id`() {
        val empty = assertThrows(IllegalArgumentException::class.java) {
            validateRegressionResults(mapOf("T-EMPTY" to "   "))
        }
        val invalid = assertThrows(IllegalArgumentException::class.java) {
            validateRegressionResults(mapOf("T-BAD" to "BLOCKED"))
        }

        assertEquals("Regression status for T-EMPTY is empty", empty.message)
        assertEquals("Invalid regression status BLOCKED for T-BAD", invalid.message)
    }
}
