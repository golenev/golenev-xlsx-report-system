package com.example.report.config

data class ColumnConfigProperties(
    val columns: Map<String, Int>,
    val regressionColumn: RegressionColumnConfig = RegressionColumnConfig()
)

data class RegressionColumnConfig(
    val key: String = "regressionStatus",
    val label: String = "Regression",
    val width: Int = 160,
    val saveOnBlur: Boolean = false
)
