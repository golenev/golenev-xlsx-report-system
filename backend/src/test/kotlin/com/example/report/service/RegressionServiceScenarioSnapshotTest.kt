package com.example.report.service

import com.example.report.entity.RegressionEntity
import com.example.report.entity.TestReportEntity
import com.example.report.model.RegressionStatus
import com.example.report.repository.RegressionRepository
import com.example.report.repository.TestReportRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class RegressionServiceScenarioSnapshotTest {

    private val regressionRepository: RegressionRepository = Mockito.mock(RegressionRepository::class.java)
    private val testReportRepository: TestReportRepository = Mockito.mock(TestReportRepository::class.java)
    private val excelExportService: ExcelExportService = Mockito.mock(ExcelExportService::class.java)
    private val service = RegressionService(regressionRepository, testReportRepository, excelExportService, fixedClock)

    /**
     * Юнит-тест фиксирует текущий контракт snapshot: при остановке регресса persisted JSON scenario
     * сохраняется строкой, которую downstream-код может десериализовать без потери steps и attachments.
     */
    @Test
    fun `stop regression stores scenario in payload as nested object`() {
        val running = RegressionEntity(
            id = 10,
            status = RegressionStatus.RUNNING,
            regressionDate = LocalDate.now(fixedClock),
            releaseName = "release-structured-scenario",
            payload = emptyMap(),
        )
        val test = TestReportEntity(testId = "UNIT-1").apply {
            category = "E2E_FOR_AUTOTEST"
            shortTitle = "Structured scenario in snapshot"
            scenario = """
                {"steps":[{"number":1,"text":"Открываем отчёт","attachments":[{"type":"text","content":"request/response"}]}]}
            """.trimIndent()
        }
        Mockito.`when`(regressionRepository.findFirstByStatusOrderByRegressionDateDesc(RegressionStatus.RUNNING)).thenReturn(running)
        Mockito.`when`(testReportRepository.findAll()).thenReturn(listOf(test))
        Mockito.`when`(regressionRepository.save(Mockito.any(RegressionEntity::class.java))).thenAnswer { it.arguments[0] }

        service.stopRegression(com.example.report.dto.RegressionStopRequest(results = mapOf("UNIT-1" to "PASSED")))

        val saved = ArgumentCaptor.forClass(RegressionEntity::class.java)
        Mockito.verify(regressionRepository).save(saved.capture())
        val payload = saved.value.payload.orEmpty()
        val tests = payload["tests"] as List<*>
        val firstTest = tests.single() as Map<*, *>
        val scenario = firstTest["scenario"]
        assertTrue(scenario is String, "Текущая реализация сохраняет persisted scenario в snapshot строкой JSON")
        val scenarioMap = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readValue(scenario as String, Map::class.java)
        val steps = scenarioMap["steps"] as List<*>
        val firstStep = steps.single() as Map<*, *>
        assertEquals("Открываем отчёт", firstStep["text"])
        val attachments = firstStep["attachments"] as List<*>
        assertEquals("request/response", (attachments.single() as Map<*, *>)["content"])
    }
}
