package com.example.e2e.http

enum class Paths(val path: String) {
    REPORTS("/api/tests"),
    REPORTS_BATCH("/api/tests/batch"),
    REGRESSION_STATE("/api/regressions"),
    REGRESSION_COMPLETE("/api/regressions/complete"),
}
