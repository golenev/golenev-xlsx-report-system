package com.example.report.service

import com.example.report.dto.RegressionStartRequest
import com.example.report.dto.RegressionStateResponse
import com.example.report.dto.RegressionStopRequest
import com.example.report.dto.RegressionReleaseSummary
import com.example.report.dto.RegressionSnapshotResponse
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
    private val excelExportService: ExcelExportService,
) {

    fun getTodayState(): RegressionStateResponse {
        val today = LocalDate.now()
        val entity = regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)
            ?: return RegressionStateResponse(RegressionStatus.IDLE, today.toString())

        return entity.toResponse()
    }

    @Transactional
    fun startRegression(request: RegressionStartRequest): RegressionStateResponse {
        val today = LocalDate.now()
        val trimmedReleaseName = request.releaseName.trim()

        val running = regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)
        if (running != null) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Regression is already running for release ${running.releaseName}",
            )
        }

        if (regressionRepository.findByReleaseName(trimmedReleaseName).isPresent) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Regression with release name $trimmedReleaseName already exists",
            )
        }

        val entity = RegressionEntity(
            status = RegressionStatus.RUNNING,
            regressionDate = today,
            releaseName = trimmedReleaseName,
            payload = emptyMap(),
        )
        regressionRepository.save(entity)
        return entity.toResponse()
    }

    @Transactional
    fun stopRegression(request: RegressionStopRequest): RegressionStateResponse {
        val running = regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No running regression to stop")
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
            "regressionDate" to running.regressionDate.toString(),
            "status" to RegressionStatus.COMPLETED.name,
            "releaseName" to running.releaseName,
            "tests" to tests.map {
                mapOf(
                    "testId" to it.testId,
                    "category" to it.category,
                    "shortTitle" to it.shortTitle,
                    "issueLink" to it.issueLink,
                    "readyDate" to it.readyDate?.toString(),
                    "generalStatus" to it.generalStatus,
                    "priority" to it.priority,
                    "scenario" to it.scenario,
                    "notes" to it.notes,
                    "regressionStatus" to results[it.testId]
                )
            }
        )

        running.status = RegressionStatus.COMPLETED
        running.payload = payload
        regressionRepository.save(running)

        return running.toResponse(emptyMap())
    }

    @Transactional
    fun cancelRegression(): RegressionStateResponse {
        val today = LocalDate.now()
        val existing = regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)
            ?: return RegressionStateResponse(RegressionStatus.IDLE, today.toString())

        if (existing.payload.isNullOrEmpty()) {
            regressionRepository.delete(existing)
            return RegressionStateResponse(RegressionStatus.IDLE, today.toString())
        }

        existing.status = RegressionStatus.COMPLETED
        regressionRepository.save(existing)
        return existing.toResponse(emptyMap())
    }

    fun listReleases(): List<RegressionReleaseSummary> {
        return regressionRepository.findAllByOrderByRegressionDateDesc()
            .map {
                RegressionReleaseSummary(
                    id = it.id,
                    name = it.releaseName,
                    regressionDate = it.regressionDate.toString(),
                    status = it.status,
                )
            }
    }

    fun requireRunningRegression() {
        val today = LocalDate.now()
        val running = regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)
        if (running == null || running.regressionDate != today) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "регресс не запущен, сначала запустите регресс",
            )
        }
    }

    @Transactional
    fun syncRunningRegressionResults(results: Map<String, String>) {
        if (results.isEmpty()) return

        val today = LocalDate.now()
        val running = regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "регресс не запущен, сначала запустите регресс")

        if (running.regressionDate != today) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "регресс не запущен, сначала запустите регресс")
        }

        val existingResults = running.extractResultsFromPayload()
        val mergedResults = existingResults.toMutableMap()
        mergedResults.putAll(results)

        val payload = mapOf(
            "regressionDate" to running.regressionDate.toString(),
            "status" to running.status.name,
            "releaseName" to running.releaseName,
            "tests" to mergedResults.entries.map { (testId, status) ->
                mapOf(
                    "testId" to testId,
                    "regressionStatus" to status,
                )
            },
        )

        running.payload = payload
        regressionRepository.save(running)
    }

    fun getRegressionSnapshot(regressionId: Long): RegressionSnapshotResponse {
        val entity = regressionRepository.findById(regressionId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Regression $regressionId not found") }

        val snapshotPayload: Map<String, Any?> = entity.payload?.toMap().orEmpty()
        return RegressionSnapshotResponse(
            id = entity.id,
            name = entity.releaseName,
            status = entity.status,
            regressionDate = entity.regressionDate.toString(),
            snapshot = snapshotPayload,
        )
    }

    fun getRegressionSnapshotWorkbook(regressionId: Long): ByteArray {
        val entity = regressionRepository.findById(regressionId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Regression $regressionId not found") }
        val snapshotPayload: Map<String, Any?> = entity.payload?.toMap().orEmpty()
        if (snapshotPayload.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Regression snapshot is empty")
        }
        return excelExportService.generateWorkbookFromSnapshot(snapshotPayload)
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
            releaseName = releaseName,
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
