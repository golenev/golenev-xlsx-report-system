package com.example.report.service

import com.example.report.dto.RegressionStartRequest
import com.example.report.dto.RegressionStopRequest
import com.example.report.entity.RegressionEntity
import com.example.report.model.RegressionStatus
import com.example.report.repository.RegressionRepository
import com.example.report.repository.TestReportRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDate
import java.util.Optional

class RegressionServiceStateUnitTest {

    private val regressionRepository: RegressionRepository = Mockito.mock(RegressionRepository::class.java)
    private val testReportRepository: TestReportRepository = Mockito.mock(TestReportRepository::class.java)
    private val excelExportService: ExcelExportService = Mockito.mock(ExcelExportService::class.java)
    private val service = RegressionService(regressionRepository, testReportRepository, excelExportService, fixedClock)

    @Test
    fun `current state is idle for no running regression and exposes only valid accumulated results`() {
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING))
            .thenReturn(null)
            .thenReturn(
                running(
                    releaseName = "release-current",
                    payload = mapOf(
                        "tests" to listOf(
                            mapOf("testId" to "T-1", "regressionStatus" to "PASSED"),
                            mapOf("testId" to "T-2"),
                            mapOf("regressionStatus" to "FAILED"),
                            "invalid",
                        ),
                    ),
                ),
            )

        val idle = service.getTodayState()
        val running = service.getTodayState()

        assertEquals(RegressionStatus.IDLE, idle.status)
        assertEquals("2026-06-28", idle.regressionDate)
        assertEquals(emptyMap<String, String>(), idle.results)
        assertEquals(null, idle.releaseName)
        assertEquals(RegressionStatus.RUNNING, running.status)
        assertEquals("release-current", running.releaseName)
        assertEquals(mapOf("T-1" to "PASSED"), running.results)
    }

    @Test
    fun `start regression rejects release name already present in history`() {
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(null)
        Mockito.`when`(regressionRepository.findByReleaseName("release-history"))
            .thenReturn(Optional.of(running("release-history").apply { status = RegressionStatus.COMPLETED }))

        assertBadRequest("Regression with release name release-history already exists") {
            service.startRegression(RegressionStartRequest(" release-history "))
        }

        Mockito.verify(regressionRepository, Mockito.never()).save(Mockito.any(RegressionEntity::class.java))
    }

    @Test
    fun `stop regression rejects request when no regression is running`() {
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(null)

        assertBadRequest("No running regression to stop") {
            service.stopRegression(RegressionStopRequest(mapOf("T-1" to "PASSED")))
        }

        Mockito.verify(testReportRepository, Mockito.never()).findAll()
    }

    @Test
    fun `cancel without running regression is idempotent idle response`() {
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(null)

        val response = service.cancelRegression()

        assertEquals(RegressionStatus.IDLE, response.status)
        assertEquals("2026-06-28", response.regressionDate)
        Mockito.verify(regressionRepository, Mockito.never()).delete(Mockito.any(RegressionEntity::class.java))
        Mockito.verify(regressionRepository, Mockito.never()).save(Mockito.any(RegressionEntity::class.java))
    }

    @Test
    fun `cancel regression with accumulated payload completes and preserves payload`() {
        val payload = mapOf<String, Any?>(
            "tests" to listOf(mapOf("testId" to "T-1", "regressionStatus" to "FAILED")),
        )
        val running = running("release-with-results", payload)
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(running)
        Mockito.`when`(regressionRepository.save(running)).thenReturn(running)

        val response = service.cancelRegression()

        assertEquals(RegressionStatus.COMPLETED, running.status)
        assertEquals(payload, running.payload)
        assertEquals(RegressionStatus.COMPLETED, response.status)
        assertEquals(emptyMap<String, String>(), response.results)
        Mockito.verify(regressionRepository, Mockito.never()).delete(running)
    }

    @Test
    fun `release history preserves repository order and maps every public field`() {
        Mockito.`when`(regressionRepository.findAllByOrderByRegressionDateDesc()).thenReturn(
            listOf(
                running("new", id = 2, date = "2026-06-28"),
                running("old", id = 1, date = "2026-06-27").apply { status = RegressionStatus.COMPLETED },
            ),
        )

        val releases = service.listReleases()

        assertEquals(listOf(2L, 1L), releases.map { it.id })
        assertEquals(listOf("new", "old"), releases.map { it.name })
        assertEquals(listOf("2026-06-28", "2026-06-27"), releases.map { it.regressionDate })
        assertEquals(listOf(RegressionStatus.RUNNING, RegressionStatus.COMPLETED), releases.map { it.status })
    }

    @Test
    fun `running regression requirement accepts today and rejects missing or stale run`() {
        val today = running("today")
        val stale = running("stale", date = "2026-06-27")
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING))
            .thenReturn(today)
            .thenReturn(null)
            .thenReturn(stale)

        service.requireRunningRegression()
        assertNotFound("регресс не запущен") { service.requireRunningRegression() }
        assertNotFound("регресс не запущен") { service.requireRunningRegression() }
    }

    @Test
    fun `empty synchronization is no op without repository lookup`() {
        service.syncRunningRegressionResults(emptyMap())

        Mockito.verifyNoInteractions(regressionRepository)
    }

    @Test
    fun `synchronization merges existing statuses overwrites duplicates and appends new results`() {
        val running = running(
            "release-sync",
            payload = mapOf(
                "tests" to listOf(
                    mapOf("testId" to "T-1", "regressionStatus" to "PASSED"),
                    mapOf("testId" to "T-2", "regressionStatus" to "FAILED"),
                    mapOf("testId" to 3, "regressionStatus" to "SKIPPED"),
                ),
            ),
        )
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(running)
        Mockito.`when`(regressionRepository.save(running)).thenReturn(running)

        service.syncRunningRegressionResults(mapOf("T-2" to "PASSED", "T-3" to "SKIPPED"))

        val tests = running.payload?.get("tests") as List<*>
        val rows = tests.map { it as Map<*, *> }
        assertEquals(listOf("T-1", "T-2", "T-3"), rows.map { it["testId"] })
        assertEquals(listOf("PASSED", "PASSED", "SKIPPED"), rows.map { it["regressionStatus"] })
        assertEquals("2026-06-28", running.payload?.get("regressionDate"))
        assertEquals("RUNNING", running.payload?.get("status"))
        assertEquals("release-sync", running.payload?.get("releaseName"))
        Mockito.verify(regressionRepository).save(running)
    }

    @Test
    fun `synchronization rejects missing and stale running regression without save`() {
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING))
            .thenReturn(null)
            .thenReturn(running("stale", date = "2026-06-27"))

        assertNotFound("регресс не запущен") {
            service.syncRunningRegressionResults(mapOf("T-1" to "PASSED"))
        }
        assertNotFound("регресс не запущен") {
            service.syncRunningRegressionResults(mapOf("T-1" to "PASSED"))
        }

        Mockito.verify(regressionRepository, Mockito.never()).save(Mockito.any(RegressionEntity::class.java))
    }

    @Test
    fun `snapshot returns copied payload and both snapshot endpoints reject unknown id`() {
        val payload = mutableMapOf<String, Any?>("status" to "COMPLETED")
        val completed = running("release-snapshot", payload = payload, id = 15).apply {
            status = RegressionStatus.COMPLETED
        }
        Mockito.`when`(regressionRepository.findById(15L)).thenReturn(Optional.of(completed))
        Mockito.`when`(regressionRepository.findById(404L)).thenReturn(Optional.empty())

        val response = service.getRegressionSnapshot(15L)

        assertEquals(15L, response.id)
        assertEquals("release-snapshot", response.name)
        assertEquals(RegressionStatus.COMPLETED, response.status)
        assertEquals("2026-06-28", response.regressionDate)
        assertEquals(payload, response.snapshot)
        payload["changed"] = true
        assertEquals(false, response.snapshot.containsKey("changed"), "Snapshot response должен копировать persisted map")
        assertNotFound("Regression 404 not found") { service.getRegressionSnapshot(404L) }
        assertNotFound("Regression 404 not found") { service.getRegressionSnapshotWorkbook(404L) }
    }

    private fun running(
        releaseName: String,
        payload: Map<String, Any?>? = emptyMap(),
        id: Long = 100,
        date: String = "2026-06-28",
    ) = RegressionEntity(
        id = id,
        status = RegressionStatus.RUNNING,
        regressionDate = LocalDate.parse(date),
        releaseName = releaseName,
        payload = payload,
    )
}
