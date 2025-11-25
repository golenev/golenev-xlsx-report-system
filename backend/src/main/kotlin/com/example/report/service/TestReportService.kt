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

    @Transactional
    fun resetRuns() {
        val reports = testReportRepository.findAll()
        reports.forEach {
            it.run1Status = null
            it.run2Status = null
            it.run3Status = null
            it.run4Status = null
            it.run5Status = null
            it.updatedAt = OffsetDateTime.now()
        }
        testReportRepository.saveAll(reports)
        testRunMetadataRepository.deleteAll()
    }

    private fun upsertSingle(item: ValidatedUpsert) {
        val runMetadata = preloadRunMetadata()
        val runTarget = resolveRunTarget(item, runMetadata)

        val applyUpdates: TestReportEntity.() -> Unit = {
            this.category = item.category
            this.shortTitle = item.shortTitle
            this.scenario = item.scenario

            item.issueLink?.let { this.issueLink = it }
            if (this.readyDate == null) {
                this.readyDate = LocalDate.now()
            }
            this.generalStatus = item.generalStatus
            item.notes?.let { this.notes = it }

            runTarget?.let { (runIndex, runDate) ->
                when (runIndex) {
                    1 -> this.run1Status = item.runStatus?.uppercase()
                    2 -> this.run2Status = item.runStatus?.uppercase()
                    3 -> this.run3Status = item.runStatus?.uppercase()
                    4 -> this.run4Status = item.runStatus?.uppercase()
                    5 -> this.run5Status = item.runStatus?.uppercase()
                    else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "runIndex must be between 1 and 5")
                }

                val meta = runMetadata[runIndex] ?: TestRunMetadataEntity(runIndex = runIndex)
                meta.runDate = runDate
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
            runIndex = item.runIndex,
            runStatus = item.runStatus?.takeIf { it.isNotBlank() }?.trim()?.uppercase(),
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
        val runIndex: Int?,
        val runStatus: String?,
        val runDate: LocalDate?
    )

    private fun resolveRunTarget(item: ValidatedUpsert, metadata: Map<Int, TestRunMetadataEntity>): RunTarget? {
        val normalizedIndex = item.runIndex
        if (normalizedIndex != null) {
            val targetMeta = metadata[normalizedIndex]
            val resolvedDate = item.runDate ?: targetMeta?.runDate ?: if (item.runStatus != null) LocalDate.now() else null
            return RunTarget(normalizedIndex, resolvedDate)
        }

        if (item.runStatus == null) {
            return null
        }

        if (item.runDate != null) {
            val existingByDate = metadata.values.firstOrNull { it.runDate == item.runDate }
            if (existingByDate != null) {
                return RunTarget(existingByDate.runIndex, existingByDate.runDate)
            }

            val firstEmpty = (1..5).firstOrNull { metadata[it]?.runDate == null }
            if (firstEmpty != null) {
                return RunTarget(firstEmpty, item.runDate)
            }
        }

        val activeIndex = determineActiveRunIndex(metadata)
        val activeMeta = metadata[activeIndex]
        val runDate = item.runDate ?: activeMeta?.runDate ?: LocalDate.now()
        return RunTarget(activeIndex, runDate)
    }

    private fun determineActiveRunIndex(metadata: Map<Int, TestRunMetadataEntity>): Int {
        val dated = metadata.values.filter { it.runDate != null }
        return dated.minByOrNull { it.runDate!! }?.runIndex
            ?: (1..5).firstOrNull { metadata[it]?.runDate == null }
            ?: 1
    }

    private fun preloadRunMetadata(): Map<Int, TestRunMetadataEntity> {
        val existing = testRunMetadataRepository.findAll().associateBy { it.runIndex }.toMutableMap()
        (1..5).forEach { index ->
            existing.putIfAbsent(index, TestRunMetadataEntity(runIndex = index))
        }
        return existing
    }

    private data class RunTarget(val index: Int, val runDate: LocalDate?)

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
