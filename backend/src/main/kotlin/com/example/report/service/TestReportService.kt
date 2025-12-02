package com.example.report.service

import com.example.report.dto.TestBatchRequest
import com.example.report.dto.TestReportItemDto
import com.example.report.dto.TestReportResponse
import com.example.report.dto.TestUpsertItem
import com.example.report.entity.TestReportEntity
import com.example.report.model.GeneralTestStatus
import com.example.report.repository.TestReportRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class TestReportService(
    private val testReportRepository: TestReportRepository,
    private val columnConfigService: ColumnConfigService
) {

    fun getReport(): TestReportResponse {
        val items = testReportRepository.findAll()
            .sortedWith { a, b -> compareTestIds(a.testId, b.testId) }
            .map { it.toDto() }
        val columns = columnConfigService.getConfig().columns
        return TestReportResponse(items = items, columnConfig = columns)
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
        val existing = testReportRepository.findByTestId(item.testId)
        if (existing.isPresent) {
            val entity = existing.get()
            entity.category = item.category
            entity.shortTitle = item.shortTitle
            entity.scenario = item.scenario

            item.issueLink?.let { entity.issueLink = it }
            entity.generalStatus = item.generalStatus
            item.notes?.let { entity.notes = it }
            entity.updatedAt = OffsetDateTime.now()
            testReportRepository.save(entity)
            return
        }

        val newEntity = TestReportEntity(testId = item.testId)
        newEntity.category = item.category
        newEntity.shortTitle = item.shortTitle
        newEntity.scenario = item.scenario
        newEntity.readyDate = item.readyDate ?: LocalDate.now()

        item.issueLink?.let { newEntity.issueLink = it }
        newEntity.generalStatus = item.generalStatus
        item.notes?.let { newEntity.notes = it }
        newEntity.updatedAt = OffsetDateTime.now()
        testReportRepository.save(newEntity)
    }

    private fun validateAndNormalize(item: TestUpsertItem): ValidatedUpsert {
        val normalizedId = normalizeTestId(item.testId)
        val category = item.category?.takeIf { it.isNotBlank() }?.trim() ?: requiredFieldMissing("category")
        val shortTitle = item.shortTitle?.takeIf { it.isNotBlank() }?.trim() ?: requiredFieldMissing("shortTitle")
        val scenario = item.scenario?.takeIf { it.isNotBlank() }?.trim() ?: requiredFieldMissing("scenario")
        val generalStatusRaw =
            item.generalStatus?.takeIf { it.isNotBlank() }?.trim() ?: requiredFieldMissing("generalStatus")

        return ValidatedUpsert(
            testId = normalizedId,
            category = category,
            shortTitle = shortTitle,
            scenario = scenario,
            issueLink = item.issueLink?.takeIf { it.isNotBlank() }?.trim(),
            generalStatus = validateGeneralStatus(generalStatusRaw),
            notes = item.notes,
            readyDate = item.readyDate
                ?.takeIf { it.isNotBlank() }
                ?.trim()
                ?.let { LocalDate.parse(it) }
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
        updatedAt = updatedAt?.toString()
    )

    private fun validateGeneralStatus(generalStatus: String): String {
        return try {
            GeneralTestStatus.requireValid(generalStatus) ?: requiredFieldMissing("generalStatus")
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
        val category: String,
        val shortTitle: String,
        val scenario: String,
        val issueLink: String?,
        val generalStatus: String,
        val notes: String?,
        val readyDate: LocalDate?
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
