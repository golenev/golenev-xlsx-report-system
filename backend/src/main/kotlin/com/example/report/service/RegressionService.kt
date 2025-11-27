package com.example.report.service

import com.example.report.dto.RegressionStateResponse
import com.example.report.dto.RegressionStopRequest
import com.example.report.dto.validateRegressionResults
import com.example.report.entity.RegressionEntity
import com.example.report.model.RegressionStatus
import com.example.report.repository.RegressionRepository
import com.example.report.repository.TestReportRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@Service
class RegressionService(
    private val regressionRepository: RegressionRepository,
    private val testReportRepository: TestReportRepository,
) {

    fun getTodayState(): RegressionStateResponse {
        val today = LocalDate.now()
        val entity = regressionRepository.findByRegressionDate(today)
            .orElse(null)
        if (entity == null) {
            return RegressionStateResponse(RegressionStatus.IDLE, today.toString())
        }

        return if (entity.status == RegressionStatus.COMPLETED) {
            RegressionStateResponse(RegressionStatus.COMPLETED, entity.regressionDate.toString())
        } else {
            entity.toResponse()
        }
    }

    @Transactional
    fun startRegression(): RegressionStateResponse {
        val today = LocalDate.now()
        val existing = regressionRepository.findByRegressionDate(today).orElse(null)

        if (existing != null) {
            existing.status = RegressionStatus.RUNNING
            regressionRepository.save(existing)
            return existing.toResponse(emptyMap())
        }

        val entity = RegressionEntity(
            status = RegressionStatus.RUNNING,
            regressionDate = today,
            payload = emptyMap(),
        )
        regressionRepository.save(entity)
        return entity.toResponse()
    }

    @Transactional
    fun stopRegression(request: RegressionStopRequest): RegressionStateResponse {
        val today = LocalDate.now()
        val results = try {
            validateRegressionResults(request.results)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
        }

        val tests = testReportRepository.findAll()
            .sortedWith { a, b -> compareTestIds(a.testId, b.testId) }

        val missing = tests.filter { results[it.testId].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Regression statuses are required for all test cases"
            )
        }

        val payload = mapOf(
            "regressionDate" to today.toString(),
            "status" to RegressionStatus.COMPLETED.name,
            "tests" to tests.map {
                mapOf(
                    "testId" to it.testId,
                    "category" to it.category,
                    "shortTitle" to it.shortTitle,
                    "issueLink" to it.issueLink,
                    "readyDate" to it.readyDate?.toString(),
                    "generalStatus" to it.generalStatus,
                    "scenario" to it.scenario,
                    "notes" to it.notes,
                    "regressionStatus" to results[it.testId]
                )
            }
        )

        val entity = regressionRepository.findByRegressionDate(today)
            .orElseGet {
                RegressionEntity(status = RegressionStatus.RUNNING, regressionDate = today)
            }

        entity.status = RegressionStatus.COMPLETED
        entity.payload = payload
        regressionRepository.save(entity)

        return entity.toResponse(emptyMap())
    }

    @Transactional
    fun cancelRegression(): RegressionStateResponse {
        val today = LocalDate.now()
        val existing = regressionRepository.findByRegressionDate(today).orElse(null)
            ?: return RegressionStateResponse(RegressionStatus.IDLE, today.toString())

        if (existing.payload.isNullOrEmpty()) {
            regressionRepository.delete(existing)
            return RegressionStateResponse(RegressionStatus.IDLE, today.toString())
        }

        existing.status = RegressionStatus.COMPLETED
        regressionRepository.save(existing)
        return existing.toResponse(emptyMap())
    }

    private fun RegressionEntity.toResponse(results: Map<String, String>? = null): RegressionStateResponse {
        val effectiveResults = when {
            results != null -> results
            status == RegressionStatus.RUNNING -> extractResultsFromPayload()
            else -> emptyMap()
        }
        return RegressionStateResponse(
            status = status,
            regressionDate = regressionDate.toString(),
            results = effectiveResults,
        )
    }

    private fun RegressionEntity.extractResultsFromPayload(): Map<String, String> {
        val payloadResults = payload?.get("tests") as? List<*> ?: return emptyMap()
        return payloadResults
            .mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val testId = map["testId"] as? String ?: return@mapNotNull null
                val regressionStatus = map["regressionStatus"] as? String ?: return@mapNotNull null
                testId to regressionStatus
            }
            .toMap()
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
        val suffix: Long?,
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
