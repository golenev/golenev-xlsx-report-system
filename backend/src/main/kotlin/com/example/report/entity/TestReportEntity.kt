package com.example.report.entity

import com.example.report.model.Priority
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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

    @Column(name = "scenario", columnDefinition = "text", nullable = false)
    var scenario: String = "",

    @Column(name = "notes", columnDefinition = "text")
    var notes: String? = null,

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null,

    @Column(name = "run_status")
    var runStatus: String? = null,
)
