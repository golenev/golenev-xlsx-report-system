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
    private companion object {
        const val DEFAULT_GENERAL_STATUS = "Готово"
        const val DEFAULT_ISSUE_LINK = "https://youtrackru/issue/"
        const val DEFAULT_NOTES = ""
    }

    private val DEFAULT_PRIORITY = Priority.MEDIUM.value

    fun getReport(): TestReportResponse {
        val items = testReportRepository.findAll()
            .sortedWith { a, b -> compareTestIds(a.testId, b.testId) }
            .map { it.toDto() }
        val columns = columnConfigService.getConfig().columns
        return TestReportResponse(items = items, columnConfig = columns)
    }

    @Transactional
    fun upsertTest(request: TestUpsertItem, forceUpdate: Boolean) {
        val normalizedId = normalizeTestId(request.testId)
        val existing = testReportRepository.findByTestId(normalizedId).orElse(null)
        val validated = validateAndNormalize(
            item = request,
            existing = existing,
            normalizedId = normalizedId,
            isRegressRunning = false,
            forceUpdate = forceUpdate,
            allowFallback = true,
        )
        upsertSingle(validated, existing, forceUpdate, false)
    }

    @Transactional
    fun upsertBatch(request: TestBatchRequest, isRegressRunning: Boolean = false, forceUpdate: Boolean = false) {
        val regressionResults = mutableMapOf<String, String>()

        request.items
            .map { item ->
                val normalizedId = normalizeTestId(item.testId)
                val existing = testReportRepository.findByTestId(normalizedId).orElse(null)
                val validated = validateAndNormalize(
                    item = item,
                    existing = existing,
                    normalizedId = normalizedId,
                    isRegressRunning = isRegressRunning,
                    forceUpdate = forceUpdate,
                    allowFallback = false,
                )
                if (isRegressRunning && validated.runStatus != null) {
                    regressionResults[validated.testId] = validated.runStatus
                }
                validated to existing
            }
            .forEach { (validated, existing) -> upsertSingle(validated, existing, forceUpdate, isRegressRunning) }

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

    private fun upsertSingle(
        item: ValidatedUpsert,
        existing: TestReportEntity?,
        forceUpdate: Boolean,
        isRegressRunning: Boolean,
    ) {
        if (existing != null) {
            val entity = existing
            entity.category = item.category
            entity.shortTitle = item.shortTitle
            entity.scenario = item.scenario

            if (forceUpdate) {
                applyManualUpdate(item.manualFields.issueLink) { entity.issueLink = it }
                applyManualUpdate(item.manualFields.generalStatus) { entity.generalStatus = it }
                applyManualUpdate(item.manualFields.priority) { entity.priority = it }
                applyManualUpdate(item.manualFields.notes) { entity.notes = it }
            }
            if (isRegressRunning) {
                entity.runStatus = item.runStatus
            }
            entity.updatedAt = OffsetDateTime.now()
            testReportRepository.save(entity)
            return
        }

        val newEntity = TestReportEntity(testId = item.testId)
        newEntity.category = item.category
        newEntity.shortTitle = item.shortTitle
        newEntity.scenario = item.scenario
        newEntity.readyDate = LocalDate.now()

        newEntity.issueLink = when {
            forceUpdate && item.manualFields.issueLink.provided ->
                item.manualFields.issueLink.value ?: DEFAULT_ISSUE_LINK
            else -> DEFAULT_ISSUE_LINK
        }
        newEntity.generalStatus = when {
            forceUpdate && item.manualFields.generalStatus.provided ->
                item.manualFields.generalStatus.value ?: DEFAULT_GENERAL_STATUS
            else -> DEFAULT_GENERAL_STATUS
        }
        newEntity.priority = when {
            forceUpdate && item.manualFields.priority.provided ->
                item.manualFields.priority.value ?: DEFAULT_PRIORITY
            else -> DEFAULT_PRIORITY
        }
        newEntity.notes = when {
            forceUpdate && item.manualFields.notes.provided ->
                item.manualFields.notes.value ?: DEFAULT_NOTES
            else -> DEFAULT_NOTES
        }
        if (isRegressRunning) {
            newEntity.runStatus = item.runStatus
        }
        newEntity.updatedAt = OffsetDateTime.now()
        testReportRepository.save(newEntity)
    }

    private fun validateAndNormalize(
        item: TestUpsertItem,
        existing: TestReportEntity?,
        normalizedId: String,
        isRegressRunning: Boolean = false,
        forceUpdate: Boolean = false,
        allowFallback: Boolean = false,
    ): ValidatedUpsert {
        val category = normalizeRequiredField(item.category, existing?.category, "category", allowFallback)
        val shortTitle = normalizeRequiredField(item.shortTitle, existing?.shortTitle, "shortTitle", allowFallback)
        val scenario = normalizeRequiredField(item.scenario, existing?.scenario, "scenario", allowFallback)

        val manualFields = ManualFields(
            issueLink = ManualField(
                provided = item.issueLink != null,
                value = if (forceUpdate) {
                    item.issueLink?.takeIf { it.isNotBlank() }?.trim() ?: existing?.issueLink
                } else {
                    existing?.issueLink
                },
            ),
            generalStatus = ManualField(
                provided = item.generalStatus != null,
                value = if (forceUpdate) {
                    item.generalStatus
                        ?.takeIf { it.isNotBlank() }
                        ?.trim()
                        ?.let { validateGeneralStatus(it) }
                        ?: existing?.generalStatus
                } else {
                    existing?.generalStatus
                },
            ),
            priority = ManualField(
                provided = item.priority != null,
                value = if (forceUpdate) {
                    item.priority
                        ?.takeIf { it.isNotBlank() }
                        ?.trim()
                        ?.let { validatePriority(it) }
                        ?: existing?.priority
                } else {
                    existing?.priority
                },
            ),
            notes = ManualField(
                provided = item.notes != null,
                value = if (forceUpdate) item.notes ?: existing?.notes else existing?.notes,
            ),
        )

        val runStatus = when {
            isRegressRunning -> validateRunStatus(item.runStatus, true)
            else -> null
        }

        return ValidatedUpsert(
            testId = normalizedId,
            category = category,
            shortTitle = shortTitle,
            scenario = scenario,
            manualFields = manualFields,
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

    private fun normalizeRequiredField(
        incoming: String?,
        existing: String?,
        fieldName: String,
        allowFallback: Boolean,
    ): String {
        val normalized = incoming?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized != null) return normalized
        if (allowFallback) {
            val fallback = existing?.trim()?.takeIf { it.isNotEmpty() }
            if (fallback != null) return fallback
        }
        requiredFieldMissing(fieldName)
    }

    private data class ValidatedUpsert(
        val testId: String,
        val category: String,
        val shortTitle: String,
        val scenario: String,
        val manualFields: ManualFields,
        val runStatus: String?,
    )

    private data class ManualField<T>(
        val provided: Boolean,
        val value: T?,
    )

    private data class ManualFields(
        val issueLink: ManualField<String?>,
        val generalStatus: ManualField<String?>,
        val priority: ManualField<String?>,
        val notes: ManualField<String?>,
    )

    private fun <T> applyManualUpdate(field: ManualField<T>, updater: (T) -> Unit) {
        if (!field.provided) return
        field.value?.let { updater(it) }
    }

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
