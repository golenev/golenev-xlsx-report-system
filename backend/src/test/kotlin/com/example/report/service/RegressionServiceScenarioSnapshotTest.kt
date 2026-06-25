package com.example.report.service

import com.example.report.entity.RegressionEntity
import com.example.report.entity.TestReportEntity
import com.example.report.model.RegressionStatus
import com.example.report.repository.RegressionRepository
import com.example.report.repository.TestReportRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegressionServiceScenarioSnapshotTest {

    private val regressionRepository: RegressionRepository = Mockito.mock(RegressionRepository::class.java)
    private val testReportRepository: TestReportRepository = Mockito.mock(TestReportRepository::class.java)
    private val excelExportService: ExcelExportService = Mockito.mock(ExcelExportService::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val service = RegressionService(regressionRepository, testReportRepository, excelExportService, objectMapper)

    /**
     * Юнит-тест фиксирует дефект двойной сериализации: при остановке регресса persisted JSON scenario
     * из test_report должен попасть в jsonb payload как вложенный объект, сохранив steps и attachments.
     */
    @Test
    fun `stop regression stores scenario in payload as nested object`() {
        val running = RegressionEntity(
            id = 10,
            status = RegressionStatus.RUNNING,
            regressionDate = LocalDate.now(),
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
        assertIs<Map<*, *>>(scenario)
        val steps = scenario["steps"] as List<*>
        val firstStep = steps.single() as Map<*, *>
        assertEquals("Открываем отчёт", firstStep["text"])
        val attachments = firstStep["attachments"] as List<*>
        assertEquals("request/response", (attachments.single() as Map<*, *>)["content"])
    }
}
