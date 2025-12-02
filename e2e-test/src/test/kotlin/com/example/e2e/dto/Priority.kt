package com.example.e2e.dto

enum class Priority(val value: String) {
    CRITICAL("Critical"),
    BLOCKER("Blocker"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    TRIVIAL("Trivial");

    companion object {
        fun random(): Priority = entries.random()
    }
}
