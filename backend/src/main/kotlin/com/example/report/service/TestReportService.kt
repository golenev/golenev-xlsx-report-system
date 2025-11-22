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
            .sortedWith { a, b -> compareTestIds(a.testId, b.testId) }
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
        val validated = validateAndNormalize(request)
        upsertSingle(validated)
    }

    @Transactional
    fun upsertBatch(request: TestBatchRequest) {
        request.items
            .map { validateAndNormalize(it) }
            .forEach { upsertSingle(it) }
    }

    @Transactional
    fun deleteTest(testId: String) {
        val entity = testReportRepository.findByTestId(testId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Test with ID $testId not found")
            }
        testReportRepository.delete(entity)
    }

    private fun upsertSingle(item: ValidatedUpsert) {
        val applyUpdates: TestReportEntity.() -> Unit = {
            category = item.category
            shortTitle = item.shortTitle
            scenario = item.scenario

            item.issueLink?.let { this.issueLink = it }
            item.readyDate?.let { this.readyDate = it }
            item.generalStatus?.let { this.generalStatus = it }
            item.notes?.let { this.notes = it }

            if (item.runIndex != null) {
                when (item.runIndex) {
                    1 -> this.run1Status = item.runStatus?.uppercase()
                    2 -> this.run2Status = item.runStatus?.uppercase()
                    3 -> this.run3Status = item.runStatus?.uppercase()
                    4 -> this.run4Status = item.runStatus?.uppercase()
                    5 -> this.run5Status = item.runStatus?.uppercase()
                    else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "runIndex must be between 1 and 5")
                }
                val meta = testRunMetadataRepository.findById(item.runIndex)
                    .orElse(TestRunMetadataEntity(runIndex = item.runIndex))
                meta.runDate = item.runDate ?: meta.runDate ?: if (item.runStatus != null) LocalDate.now() else null
                testRunMetadataRepository.save(meta)
            }
        }

        val existing = testReportRepository.findByTestId(item.testId)
        if (existing.isPresent) {
            val entity = existing.get()
            applyUpdates(entity)
            entity.updatedAt = OffsetDateTime.now()
            testReportRepository.save(entity)
            return
        }

        val newEntity = TestReportEntity(testId = item.testId)
        applyUpdates(newEntity)
        newEntity.updatedAt = OffsetDateTime.now()
        testReportRepository.save(newEntity)
    }

    private fun validateAndNormalize(item: TestUpsertItem): ValidatedUpsert {
        val normalizedId = normalizeTestId(item.testId)
        val category = item.category?.takeIf { it.isNotBlank() }?.trim()
        val shortTitle = item.shortTitle?.takeIf { it.isNotBlank() }?.trim()
        val scenario = item.scenario?.takeIf { it.isNotBlank() }?.trim()

        return ValidatedUpsert(
            testId = normalizedId,
            category = category,
            shortTitle = shortTitle,
            scenario = scenario,
            issueLink = item.issueLink?.takeIf { it.isNotBlank() }?.trim(),
            readyDate = item.readyDate?.takeIf { it.isNotBlank() }?.trim()?.let { LocalDate.parse(it) },
            generalStatus = validateGeneralStatus(item.generalStatus?.takeIf { it.isNotBlank() }?.trim()),
            notes = item.notes,
            runIndex = item.runIndex,
            runStatus = item.runStatus?.takeIf { it.isNotBlank() }?.trim(),
            runDate = item.runDate?.takeIf { it.isNotBlank() }?.trim()?.let { LocalDate.parse(it) }
        )
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

    private fun normalizeTestId(testId: String?): String {
        val normalizedId = testId?.trim().orEmpty()
        if (normalizedId.isEmpty()) {
            requiredFieldMissing("testId")
        }
        return normalizedId
    }

    private fun requiredFieldMissing(fieldName: String): Nothing {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Required field $fieldName is missing")
    }

    private data class ValidatedUpsert(
        val testId: String,
        val category: String?,
        val shortTitle: String?,
        val scenario: String?,
        val issueLink: String?,
        val readyDate: LocalDate?,
        val generalStatus: String?,
        val notes: String?,
        val runIndex: Int?,
        val runStatus: String?,
        val runDate: LocalDate?
    )

    private fun compareTestIds(a: String, b: String): Int {
        val left = ParsedTestId.from(a)
        val right = ParsedTestId.from(b)

        if (left.numericPart != null && right.numericPart != null) {
            val numericComparison = left.numericPart.compareTo(right.numericPart)
            if (numericComparison != 0) {
                return numericComparison
            }

            if (left.suffix == null && right.suffix != null) return -1
            if (left.suffix != null && right.suffix == null) return 1

            if (left.suffix != null && right.suffix != null) {
                val suffixComparison = left.suffix.compareTo(right.suffix)
                if (suffixComparison != 0) {
                    return suffixComparison
                }
            }
        }

        return left.original.compareTo(right.original, ignoreCase = true)
    }

    private data class ParsedTestId(
        val original: String,
        val numericPart: Long?,
        val suffix: Long?
    ) {
        companion object {
            private val pattern = Regex("^(\\d+)(?:-(\\d+))?$")

            fun from(raw: String): ParsedTestId {
                val trimmed = raw.trim()
                val match = pattern.matchEntire(trimmed)
                return if (match != null) {
                    val numeric = match.groupValues[1].toLong()
                    val suffix = match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toLong()
                    ParsedTestId(trimmed, numeric, suffix)
                } else {
                    ParsedTestId(trimmed, null, null)
                }
            }
        }
    }
}
