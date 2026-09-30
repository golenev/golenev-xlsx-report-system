package com.example.report.entity

import com.example.report.dto.ScenarioRequest
import com.example.report.model.Priority
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "test_report")
data class TestReportEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "test_id", nullable = false, unique = true)
    var testId: String,

    @Column(nullable = false)
    var category: String = "",

    @Column(name = "short_title", nullable = false)
    var shortTitle: String = "",

    @Column(name = "issue_link")
    var issueLink: String? = null,

    @Column(name = "ready_date")
    var readyDate: LocalDate? = null,

    @Column(name = "general_status")
    var generalStatus: String? = null,

    @Column(name = "priority", nullable = false)
    var priority: String = Priority.MEDIUM.value,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scenario", columnDefinition = "jsonb", nullable = false)
    var scenario: ScenarioRequest = ScenarioRequest(),

    @Column(name = "notes", columnDefinition = "text")
    var notes: String? = null,

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null,

)
