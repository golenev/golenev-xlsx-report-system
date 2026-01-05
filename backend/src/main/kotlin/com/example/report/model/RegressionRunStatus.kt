package com.example.report.model

enum class RegressionRunStatus(val value: String) {
    PASSED("passed"),
    FAILED("failed"),
    SKIPPED("skipped");

    companion object {
        fun requireValid(value: String?): String? {
            if (value == null) return null

            val normalized = value.trim()
            if (normalized.isEmpty()) return null

            val matched = values().firstOrNull { candidate ->
                candidate.name.equals(normalized, ignoreCase = true) ||
                    candidate.value.equals(normalized, ignoreCase = true)
            } ?: throw IllegalArgumentException("Неправильный статус прогона: $value")

            return matched.name
        }
    }
}
