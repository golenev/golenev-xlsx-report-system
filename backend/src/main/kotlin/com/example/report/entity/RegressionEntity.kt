package com.example.report.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "regressions")
data class RegressionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "regression_date", nullable = false, unique = true)
    var regressionDate: LocalDate,

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    var payload: String
)
