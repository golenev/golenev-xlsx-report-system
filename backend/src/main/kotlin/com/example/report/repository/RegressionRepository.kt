package com.example.report.repository

import com.example.report.entity.RegressionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface RegressionRepository : JpaRepository<RegressionEntity, Long> {
    fun findByRegressionDate(regressionDate: LocalDate): Optional<RegressionEntity>
}
