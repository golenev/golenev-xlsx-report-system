package com.example.report.service

import com.example.report.dto.RegressionRecordDto
import com.example.report.dto.RegressionRequest
import com.example.report.dto.TestBatchRequest
import com.example.report.dto.TestReportItemDto
import com.example.report.dto.TestReportResponse
import com.example.report.dto.TestUpsertItem
import com.example.report.entity.RegressionEntity
import com.example.report.entity.TestReportEntity
import com.example.report.model.GeneralTestStatus
import com.example.report.repository.RegressionRepository
import com.example.report.repository.TestReportRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class TestReportService(
    private val testReportRepository: TestReportRepository,
    private val regressionRepository: RegressionRepository,
    private val columnConfigService: ColumnConfigService,
    private val objectMapper: ObjectMapper
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

    @Transactional
    fun resetRuns() {
        val reports = testReportRepository.findAll()
        reports.forEach {
            it.regressionStatus = null
            it.updatedAt = OffsetDateTime.now()
        }
        testReportRepository.saveAll(reports)
        regressionRepository.deleteAll()
    }

    @Transactional
    fun saveRegression(request: RegressionRequest): RegressionRecordDto {
        val regressionDate = request.regressionDate?.takeIf { it.isNotBlank() }
            ?.trim()
            ?.let {
                runCatching { LocalDate.parse(it) }.getOrElse {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid regressionDate format")
                }
            }
            ?: requiredFieldMissing("regressionDate")
        val payload = request.payload ?: requiredFieldMissing("payload")
        val payloadJson = objectMapper.writeValueAsString(payload)

        val entity = regressionRepository.findByRegressionDate(regressionDate)
            .orElse(RegressionEntity(regressionDate = regressionDate, payload = payloadJson))
        entity.payload = payloadJson

        val saved = regressionRepository.save(entity)
        return saved.toDto()
    }

    fun getRegressions(): List<RegressionRecordDto> {
        return regressionRepository.findAllByOrderByRegressionDateDesc().map { it.toDto() }
    }

    private fun upsertSingle(item: ValidatedUpsert) {
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
            item.regressionStatus?.let { this.regressionStatus = it }
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
            regressionStatus = item.regressionStatus?.takeIf { it.isNotBlank() }?.trim()?.uppercase()
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
        regressionStatus = regressionStatus,
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
        val regressionStatus: String?
    )

    private fun RegressionEntity.toDto(): RegressionRecordDto = RegressionRecordDto(
        id = id,
        regressionDate = regressionDate.toString(),
        payload = objectMapper.readTree(payload)
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
