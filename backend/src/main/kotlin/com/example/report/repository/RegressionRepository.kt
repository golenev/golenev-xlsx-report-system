package com.example.report.repository

import com.example.report.entity.RegressionEntity
import com.example.report.model.RegressionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RegressionRepository : JpaRepository<RegressionEntity, Long> {
    fun findByReleaseName(releaseName: String): Optional<RegressionEntity>

    fun findFirstByStatusOrderByRegressionDateDesc(status: RegressionStatus): RegressionEntity?
}
