package com.example.report.service

import com.example.report.dto.TestBatchRequest
import com.example.report.dto.TestReportItemDto
import com.example.report.dto.TestReportResponse
import com.example.report.dto.TestUpsertItem
import com.example.report.entity.TestReportEntity
import com.example.report.model.GeneralTestStatus
import com.example.report.model.RegressionRunStatus
import com.example.report.model.Priority
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
    private val columnConfigService: ColumnConfigService,
    private val regressionService: RegressionService,
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
        val normalizedId = normalizeTestId(request.testId)
        val existing = testReportRepository.findByTestId(normalizedId).orElse(null)
        val validated = validateAndNormalize(request, existing, normalizedId)
        upsertSingle(validated, existing)
    }

    @Transactional
    fun upsertBatch(request: TestBatchRequest, isRegressRunning: Boolean = false) {
        val regressionResults = mutableMapOf<String, String>()

        request.items
            .map { item ->
                val normalizedId = normalizeTestId(item.testId)
                val existing = testReportRepository.findByTestId(normalizedId).orElse(null)
                val validated = validateAndNormalize(item, existing, normalizedId, isRegressRunning)
                if (isRegressRunning && validated.runStatus != null) {
                    regressionResults[validated.testId] = validated.runStatus
                }
                validated to existing
            }
            .forEach { (validated, existing) -> upsertSingle(validated, existing) }

        if (isRegressRunning && regressionResults.isNotEmpty()) {
            regressionService.syncRunningRegressionResults(regressionResults)
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

    private fun upsertSingle(item: ValidatedUpsert, existing: TestReportEntity?) {
        if (existing != null) {
            val entity = existing
            entity.category = item.category
            entity.shortTitle = item.shortTitle
            entity.scenario = item.scenario

            item.issueLink?.let { entity.issueLink = it }
            entity.generalStatus = item.generalStatus
            entity.priority = item.priority
            item.notes?.let { entity.notes = it }
            entity.runStatus = item.runStatus
            entity.updatedAt = OffsetDateTime.now()
            testReportRepository.save(entity)
            return
        }

        val newEntity = TestReportEntity(testId = item.testId, priority = item.priority)
        newEntity.category = item.category
        newEntity.shortTitle = item.shortTitle
        newEntity.scenario = item.scenario
        newEntity.readyDate = item.readyDate ?: LocalDate.now()

        item.issueLink?.let { newEntity.issueLink = it }
        newEntity.generalStatus = item.generalStatus
        item.notes?.let { newEntity.notes = it }
        newEntity.runStatus = item.runStatus
        newEntity.updatedAt = OffsetDateTime.now()
        testReportRepository.save(newEntity)
    }

    private fun validateAndNormalize(
        item: TestUpsertItem,
        existing: TestReportEntity?,
        normalizedId: String,
        isRegressRunning: Boolean = false,
    ): ValidatedUpsert {
        val category = item.category
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?: existing?.category?.takeIf { it.isNotBlank() }
            ?: requiredFieldMissing("category")
        val shortTitle = item.shortTitle
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?: existing?.shortTitle?.takeIf { it.isNotBlank() }
            ?: requiredFieldMissing("shortTitle")
        val scenario = item.scenario
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?: existing?.scenario?.takeIf { it.isNotBlank() }
            ?: requiredFieldMissing("scenario")
        val generalStatusRaw =
            item.generalStatus
                ?.takeIf { it.isNotBlank() }
                ?.trim()
                ?: existing?.generalStatus?.takeIf { it.isNotBlank() }
                ?: requiredFieldMissing("generalStatus")
        val priorityRaw =
            item.priority
                ?.takeIf { it.isNotBlank() }
                ?.trim()
                ?: existing?.priority?.takeIf { it.isNotBlank() }
                ?: requiredFieldMissing("priority")

        val runStatus = when {
            isRegressRunning -> validateRunStatus(item.runStatus, true)
            item.runStatus != null -> validateRunStatus(item.runStatus, false)
            else -> existing?.runStatus
        }

        return ValidatedUpsert(
            testId = normalizedId,
            category = category,
            shortTitle = shortTitle,
            scenario = scenario,
            issueLink = item.issueLink?.takeIf { it.isNotBlank() }?.trim(),
            generalStatus = validateGeneralStatus(generalStatusRaw),
            priority = validatePriority(priorityRaw),
            notes = item.notes,
            readyDate = item.readyDate
                ?.takeIf { it.isNotBlank() }
                ?.trim()
                ?.let { LocalDate.parse(it) },
            runStatus = runStatus,
        )
    }

    private fun TestReportEntity.toDto(): TestReportItemDto = TestReportItemDto(
        testId = testId,
        category = category,
        shortTitle = shortTitle,
        issueLink = issueLink,
        readyDate = readyDate,
        generalStatus = generalStatus,
        priority = priority,
        scenario = scenario,
        notes = notes,
        updatedAt = updatedAt?.toString(),
        runStatus = runStatus,
    )

    private fun validateGeneralStatus(generalStatus: String): String {
        return try {
            GeneralTestStatus.requireValid(generalStatus) ?: requiredFieldMissing("generalStatus")
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid status")
        }
    }

    private fun validatePriority(priority: String): String {
        return try {
            Priority.requireValid(priority) ?: requiredFieldMissing("priority")
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid priority")
        }
    }

    private fun validateRunStatus(runStatus: String?, required: Boolean): String? {
        val validated = try {
            RegressionRunStatus.requireValid(runStatus)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid run status")
        }

        if (required && validated == null) {
            requiredFieldMissing("runStatus")
        }

        return validated
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
        val priority: String,
        val notes: String?,
        val readyDate: LocalDate?,
        val runStatus: String?,
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
