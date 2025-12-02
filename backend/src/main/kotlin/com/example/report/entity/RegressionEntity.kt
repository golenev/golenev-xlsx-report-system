package com.example.report.entity

import com.example.report.model.RegressionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate

@Entity
@Table(name = "regressions")
data class RegressionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RegressionStatus,

    @Column(name = "regression_date", nullable = false)
    var regressionDate: LocalDate,

    @Column(name = "release_name", nullable = false, unique = true)
    var releaseName: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var payload: Map<String, Any?>? = null,
)
