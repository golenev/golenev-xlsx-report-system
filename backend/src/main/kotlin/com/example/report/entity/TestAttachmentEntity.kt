package com.example.report.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "test_attachment")
data class TestAttachmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "test_id", nullable = false)
    val testId: String,

    @Column(name = "step_number", nullable = false)
    val stepNumber: Int,

    @Column
    val name: String? = null,

    @Column(name = "media_type")
    val mediaType: String? = null,

    @Column(columnDefinition = "text", nullable = false)
    val content: String,

    @Column
    val source: String? = null,

    @Column(name = "size_bytes")
    val sizeBytes: Long? = null,
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", referencedColumnName = "test_id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    lateinit var testReport: TestReportEntity
}
