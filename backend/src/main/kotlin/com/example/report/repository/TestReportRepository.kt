package com.example.report.repository

import com.example.report.entity.TestReportEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TestReportRepository : JpaRepository<TestReportEntity, Long> {
    fun findByTestId(testId: String): Optional<TestReportEntity>
}
