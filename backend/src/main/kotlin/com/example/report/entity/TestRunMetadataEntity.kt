package com.example.report.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "test_run_metadata")
data class TestRunMetadataEntity(
    @Id
    @Column(name = "run_index")
    val runIndex: Int = 0,

    @Column(name = "run_date")
    var runDate: LocalDate? = null
)
