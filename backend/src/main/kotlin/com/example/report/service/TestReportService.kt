package com.example.report.service

import com.example.report.dto.TestBatchRequest
import com.example.report.dto.TestReportItemDto
import com.example.report.dto.TestReportResponse
import com.example.report.dto.TestRunMetaDto
import com.example.report.dto.TestUpsertItem
import com.example.report.entity.TestReportEntity
import com.example.report.entity.TestRunMetadataEntity
import com.example.report.model.GeneralTestStatus
import com.example.report.repository.TestReportRepository
import com.example.report.repository.TestRunMetadataRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class TestReportService(
    private val testReportRepository: TestReportRepository,
    private val testRunMetadataRepository: TestRunMetadataRepository,
    private val columnConfigService: ColumnConfigService
) {

    fun getReport(): TestReportResponse {
        val items = testReportRepository.findAll()
            .sortedBy { it.id }
            .map { it.toDto() }
        val runs = (1..5).map { index ->
            val meta = testRunMetadataRepository.findById(index).orElse(TestRunMetadataEntity(index, null))
            TestRunMetaDto(runIndex = index, runDate = meta.runDate)
        }
        val columns = columnConfigService.getConfig().columns
        return TestReportResponse(items = items, runs = runs, columnConfig = columns)
    }

    @Transactional
    fun upsertTest(request: TestUpsertItem) {
        val normalizedId = normalizeTestId(request.testId)
        upsertSingle(
            testId = normalizedId,
            category = request.category,
            shortTitle = request.shortTitle,
            issueLink = request.issueLink,
            readyDate = request.readyDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
            generalStatus = validateGeneralStatus(request.generalStatus),
            scenario = request.scenario,
            notes = request.notes,
            runIndex = request.runIndex,
            runStatus = request.runStatus?.takeIf { it.isNotBlank() },
            runDate = request.runDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        )
    }

    @Transactional
    fun upsertBatch(request: TestBatchRequest, rewriteForce: Boolean) {
        val normalizedItems = request.items.map { item ->
            item.copy(testId = normalizeTestId(item.testId))
        }

        if (!rewriteForce) {
            val existingIds = testReportRepository.findAllByTestIdIn(normalizedItems.map { it.testId })
                .map { it.testId }
                .toSet()
            if (existingIds.isNotEmpty()) {
                val idsList = existingIds.joinToString(", ") { it }
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Test with ID(s) $idsList already exist"
                )
            }
        }

        normalizedItems.forEach { item ->
            upsertSingle(
                testId = item.testId,
                category = item.category,
                shortTitle = item.shortTitle,
                issueLink = item.issueLink,
                readyDate = item.readyDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                generalStatus = validateGeneralStatus(item.generalStatus),
                scenario = item.scenario,
                notes = item.notes,
                runIndex = item.runIndex,
                runStatus = item.runStatus?.takeIf { it.isNotBlank() },
                runDate = item.runDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
            )
        }
    }

    @Transactional
    fun deleteTest(testId: String) {
        val entity = testReportRepository.findByTestId(testId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Test with ID $testId not found")
            }
        testReportRepository.delete(entity)
    }

    private fun upsertSingle(
        testId: String,
        category: String?,
        shortTitle: String?,
        issueLink: String?,
        readyDate: LocalDate?,
        generalStatus: String?,
        scenario: String?,
        notes: String?,
        runIndex: Int?,
        runStatus: String?,
        runDate: LocalDate?
    ) {
        val applyUpdates: TestReportEntity.() -> Unit = {
            category?.let { this.category = it }
            shortTitle?.let { this.shortTitle = it }
            issueLink?.let { this.issueLink = it }
            readyDate?.let { this.readyDate = it }
            generalStatus?.let { this.generalStatus = it }
            scenario?.let { this.scenario = it }
            notes?.let { this.notes = it }

            if (runIndex != null) {
                when (runIndex) {
                    1 -> this.run1Status = runStatus?.uppercase()
                    2 -> this.run2Status = runStatus?.uppercase()
                    3 -> this.run3Status = runStatus?.uppercase()
                    4 -> this.run4Status = runStatus?.uppercase()
                    5 -> this.run5Status = runStatus?.uppercase()
                    else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "runIndex must be between 1 and 5")
                }
                val meta = testRunMetadataRepository.findById(runIndex)
                    .orElse(TestRunMetadataEntity(runIndex = runIndex))
                meta.runDate = runDate ?: meta.runDate ?: if (runStatus != null) LocalDate.now() else null
                testRunMetadataRepository.save(meta)
            }
        }

        val existing = testReportRepository.findByTestId(testId)
        if (existing.isPresent) {
            val entity = existing.get()
            applyUpdates(entity)
            entity.updatedAt = OffsetDateTime.now()
            testReportRepository.save(entity)
            return
        }

        val newEntity = TestReportEntity(testId = testId)
        applyUpdates(newEntity)
        newEntity.updatedAt = OffsetDateTime.now()
        testReportRepository.save(newEntity)
    }

    private fun TestReportEntity.toDto(): TestReportItemDto = TestReportItemDto(
        testId = testId,
        category = category,
        shortTitle = shortTitle,
        issueLink = issueLink,
        readyDate = readyDate,
        generalStatus = generalStatus,
        scenario = scenario,
        notes = notes,
        runStatuses = listOf(run1Status, run2Status, run3Status, run4Status, run5Status),
        updatedAt = updatedAt?.toString()
    )

    private fun validateGeneralStatus(generalStatus: String?): String? {
        return try {
            GeneralTestStatus.requireValid(generalStatus)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid status")
        }
    }

    private fun normalizeTestId(testId: String): String {
        val normalizedId = testId.trim()
        if (normalizedId.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "testId must not be blank")
        }
        return normalizedId
    }
}
