package com.example.report.model

enum class Priority(val value: String) {
    CRITICAL("Critical"),
    BLOCKER("Blocker"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    TRIVIAL("Trivial");

    companion object {
        private val byValue = values().associateBy { it.value.lowercase() }

        fun fromValue(value: String?): Priority? = value?.let { byValue[it.lowercase()] }

        fun requireValid(value: String?): String? {
            if (value == null) return null
            return fromValue(value)?.value
                ?: throw IllegalArgumentException("Invalid priority: $value")
        }
    }
}
