package com.example.report.service

import com.example.report.dto.ScenarioAttachmentRequest
import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import com.example.report.dto.TestUpsertItem
import com.example.report.entity.TestReportEntity
import com.example.report.repository.TestReportRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals

class TestReportServicePostUnitTest {

    private val testReportRepository: TestReportRepository = Mockito.mock(TestReportRepository::class.java)
    private val columnConfigService: ColumnConfigService = Mockito.mock(ColumnConfigService::class.java)
    private val regressionService: RegressionService = Mockito.mock(RegressionService::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val service = TestReportService(testReportRepository, columnConfigService, regressionService, objectMapper)

    /**
     * Юнит-тест моделирует сервисный слой POST /api/tests для нового testId: валидный structured scenario
     * с вложением должен нормализоваться в JSON-объект steps, который затем можно без потерь десериализовать.
     */
    @Test
    fun `upsert test persists structured scenario with attachments as json object`() {
        Mockito.`when`(testReportRepository.findByTestId("UNIT-POST-1")).thenReturn(Optional.empty())
        Mockito.`when`(testReportRepository.save(Mockito.any(TestReportEntity::class.java))).thenAnswer { it.arguments[0] }

        service.upsertTest(
            TestUpsertItem(
                testId = " UNIT-POST-1 ",
                category = " E2E_FOR_AUTOTEST ",
                shortTitle = " Structured scenario unit ",
                scenario = ScenarioRequest(
                    steps = listOf(
                        ScenarioStepRequest(
                            number = 1,
                            text = "  Отправляем POST /api/tests  ",
                            attachments = listOf(ScenarioAttachmentRequest(type = "text", content = "request payload")),
                        ),
                    ),
                ),
            ),
            forceUpdate = false,
        )

        val saved = ArgumentCaptor.forClass(TestReportEntity::class.java)
        Mockito.verify(testReportRepository).save(saved.capture())
        val scenario = objectMapper.readValue(saved.value.scenario, Map::class.java)
        val steps = scenario["steps"] as List<*>
        val firstStep = steps.single() as Map<*, *>
        assertEquals("Отправляем POST /api/tests", firstStep["text"])
        val attachments = firstStep["attachments"] as List<*>
        assertEquals("request payload", (attachments.single() as Map<*, *>)["content"])
    }

    /**
     * Юнит-тест применяет граничные значения к массиву steps: пустой шаг отбрасывается, но если после фильтрации
     * остаётся валидный шаг, POST /api/tests не должен ошибочно отклонять весь scenario.
     */
    @Test
    fun `upsert test filters empty scenario steps and keeps valid boundary step`() {
        Mockito.`when`(testReportRepository.findByTestId("UNIT-POST-BOUNDARY")).thenReturn(Optional.empty())
        Mockito.`when`(testReportRepository.save(Mockito.any(TestReportEntity::class.java))).thenAnswer { it.arguments[0] }

        service.upsertTest(
            TestUpsertItem(
                testId = "UNIT-POST-BOUNDARY",
                category = "API",
                shortTitle = "Boundary scenario",
                scenario = ScenarioRequest(
                    steps = listOf(
                        ScenarioStepRequest(number = 1, text = "   ", attachments = emptyList()),
                        ScenarioStepRequest(number = 2, text = "Минимальный валидный шаг", attachments = emptyList()),
                    ),
                ),
            ),
            forceUpdate = false,
        )

        val saved = ArgumentCaptor.forClass(TestReportEntity::class.java)
        Mockito.verify(testReportRepository).save(saved.capture())
        val scenario = objectMapper.readValue(saved.value.scenario, Map::class.java)
        val steps = scenario["steps"] as List<*>
        assertEquals(1, steps.size)
        assertEquals(2, (steps.single() as Map<*, *>)["number"])
    }
}
