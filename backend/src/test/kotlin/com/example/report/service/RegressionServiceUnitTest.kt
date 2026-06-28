package com.example.report.service

import com.example.report.dto.RegressionStartRequest
import com.example.report.dto.RegressionStopRequest
import com.example.report.entity.RegressionEntity
import com.example.report.entity.TestReportEntity
import com.example.report.model.RegressionStatus
import com.example.report.repository.RegressionRepository
import com.example.report.repository.TestReportRepository
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDate
import java.util.Optional

class RegressionServiceUnitTest {

    private val regressionRepository: RegressionRepository = Mockito.mock(RegressionRepository::class.java)
    private val testReportRepository: TestReportRepository = Mockito.mock(TestReportRepository::class.java)
    private val excelExportService: ExcelExportService = Mockito.mock(ExcelExportService::class.java)
    private val service = RegressionService(regressionRepository, testReportRepository, excelExportService, fixedClock)

    /**
     * Позитивный unit-тест проверяет старт регресса на границе ввода: releaseName trim-нормализуется,
     * дата берётся из Clock, а persisted entity остаётся RUNNING.
     */
    @Test
    fun `start regression trims unique release and persists running state for today`() {
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(null)
        Mockito.`when`(regressionRepository.findByReleaseName("release-1")).thenReturn(Optional.empty())
        Mockito.`when`(regressionRepository.save(Mockito.any(RegressionEntity::class.java))).thenAnswer { it.arguments[0] }

        val response = service.startRegression(RegressionStartRequest(" release-1 "))

        val saved = ArgumentCaptor.forClass(RegressionEntity::class.java)
        Mockito.verify(regressionRepository).save(saved.capture())
        assertEquals(RegressionStatus.RUNNING, response.status, "Ответ после старта должен быть RUNNING")
        assertEquals("2026-06-28", response.regressionDate, "Дата регресса должна быть детерминирована fixedClock")
        assertEquals("release-1", saved.value.releaseName, "releaseName должен быть сохранён без пробелов по краям")
    }

    /**
     * Негативный unit-тест проверяет бизнес-ограничение: нельзя запустить второй регресс, пока есть RUNNING.
     */
    @Test
    fun `start regression rejects duplicate running regression`() {
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(running("release-active"))

        assertBadRequest("Regression is already running") { service.startRegression(RegressionStartRequest("release-new")) }
        Mockito.verify(regressionRepository, Mockito.never()).save(Mockito.any(RegressionEntity::class.java))
    }

    /**
     * Позитивный unit-тест проверяет stopRegression: результаты приводятся к upper-case, тесты сортируются численно,
     * snapshot получает COMPLETED и сохраняет статусы по testId.
     */
    @Test
    fun `stop regression stores normalized sorted payload and completes entity`() {
        val running = running("release-stop")
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(running)
        Mockito.`when`(testReportRepository.findAll()).thenReturn(listOf(test("10"), test("2-1"), test("2")))
        Mockito.`when`(regressionRepository.save(Mockito.any(RegressionEntity::class.java))).thenAnswer { it.arguments[0] }

        val response = service.stopRegression(RegressionStopRequest(mapOf("2" to " passed ", "2-1" to "FAILED", "10" to "skipped")))

        val saved = ArgumentCaptor.forClass(RegressionEntity::class.java)
        Mockito.verify(regressionRepository).save(saved.capture())
        assertEquals(RegressionStatus.COMPLETED, response.status, "После остановки ответ должен быть COMPLETED")
        assertEquals(RegressionStatus.COMPLETED, saved.value.status, "Entity должна быть переведена в COMPLETED")
        val tests = saved.value.payload?.get("tests") as List<*>
        val rows = tests.map { it as Map<*, *> }
        assertEquals(listOf("2", "2-1", "10"), rows.map { it["testId"] }, "Snapshot должен сортировать testId численно и по суффиксу")
        assertEquals(listOf("PASSED", "FAILED", "SKIPPED"), rows.map { it["regressionStatus"] }, "Статусы должны быть нормализованы")
    }

    /**
     * Негативный edge-тест проверяет полноту результатов: если хотя бы один тест без статуса, snapshot не сохраняется.
     */
    @Test
    fun `stop regression rejects missing status for any test case`() {
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(running("release-missing"))
        Mockito.`when`(testReportRepository.findAll()).thenReturn(listOf(test("1"), test("2")))

        assertBadRequest("Regression statuses are required for all test cases") {
            service.stopRegression(RegressionStopRequest(mapOf("1" to "PASSED")))
        }
        Mockito.verify(regressionRepository, Mockito.never()).save(Mockito.any(RegressionEntity::class.java))
    }

    /**
     * Позитивный edge-тест проверяет cancelRegression: пустой RUNNING без snapshot удаляется, а наружу возвращается IDLE.
     */
    @Test
    fun `cancel regression deletes empty running regression and returns idle`() {
        val running = running("release-cancel", payload = emptyMap())
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(running)

        val response = service.cancelRegression()

        Mockito.verify(regressionRepository).delete(running)
        assertEquals(RegressionStatus.IDLE, response.status, "Cancel пустого RUNNING должен вернуть IDLE")
        assertEquals("2026-06-28", response.regressionDate, "Дата IDLE-ответа должна быть сегодняшней из fixedClock")
    }

    /**
     * Позитивный unit-тест проверяет делегирование workbook для snapshot и негативную ветку пустого payload.
     */
    @Test
    fun `get regression snapshot workbook delegates non empty payload and rejects empty payload`() {
        val payload = mapOf<String, Any?>("tests" to emptyList<Map<String, String>>())
        Mockito.`when`(regressionRepository.findById(1L)).thenReturn(Optional.of(running("release-xlsx", payload = payload)))
        Mockito.`when`(excelExportService.generateWorkbookFromSnapshot(payload)).thenReturn(byteArrayOf(1, 2, 3))

        assertArrayEquals(byteArrayOf(1, 2, 3), service.getRegressionSnapshotWorkbook(1L), "Workbook bytes должны вернуться из ExcelExportService без изменений")

        Mockito.`when`(regressionRepository.findById(2L)).thenReturn(Optional.of(running("release-empty", payload = emptyMap())))
        assertBadRequest("Regression snapshot is empty") { service.getRegressionSnapshotWorkbook(2L) }
    }

    private fun running(releaseName: String, payload: Map<String, Any?>? = emptyMap()) = RegressionEntity(
        id = 100,
        status = RegressionStatus.RUNNING,
        regressionDate = LocalDate.parse("2026-06-28"),
        releaseName = releaseName,
        payload = payload,
    )

    private fun test(testId: String) = TestReportEntity(testId = testId).apply {
        category = "API"
        shortTitle = "Title $testId"
        scenario = "scenario $testId"
    }
}
