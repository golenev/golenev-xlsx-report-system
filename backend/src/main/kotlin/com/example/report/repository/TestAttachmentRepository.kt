package com.example.report.repository

import com.example.report.entity.TestAttachmentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TestAttachmentRepository : JpaRepository<TestAttachmentEntity, Long> {
    fun findAllByTestIdIn(testIds: Collection<String>): List<TestAttachmentEntity>
    fun deleteAllByTestId(testId: String)
}
