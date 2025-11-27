package com.example.report.repository

import com.example.report.entity.TestRunMetadataEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TestRunMetadataRepository : JpaRepository<TestRunMetadataEntity, Int>
