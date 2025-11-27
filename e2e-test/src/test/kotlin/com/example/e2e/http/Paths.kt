package com.example.e2e.http

enum class Paths(val path: String) {
    REPORTS("/api/tests"),
    REPORTS_BATCH("/api/tests/batch"),
    RUNS_RESET("/api/tests/runs/reset"),
}
