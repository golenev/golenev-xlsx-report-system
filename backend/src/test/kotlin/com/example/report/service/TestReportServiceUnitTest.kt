package com.example.report.service

import com.example.report.config.ColumnConfigProperties
import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import com.example.report.dto.TestBatchRequest
import com.example.report.dto.TestUpsertItem
import com.example.report.entity.TestReportEntity
import com.example.report.repository.TestReportRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDate
import java.util.Optional

class TestReportServiceUnitTest {

    private val testReportRepository: TestReportRepository = Mockito.mock(TestReportRepository::class.java)
    private val columnConfigService: ColumnConfigService = Mockito.mock(ColumnConfigService::class.java)
    private val regressionService: RegressionService = Mockito.mock(RegressionService::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val service = TestReportService(testReportRepository, columnConfigService, regressionService, objectMapper, fixedClock)

    /**
     * Позитивный unit-тест проверяет сортировку отчёта по числовому testId, суффиксам и fallback-десериализацию
     * legacy scenario: при падении сразу видно, какой порядок или текст сценария сломался.
     */
    @Test
    fun `get report sorts numeric ids before suffixed ids and converts legacy scenario text`() {
        Mockito.`when`(columnConfigService.getConfig()).thenReturn(ColumnConfigProperties(mapOf("testId" to 120), mapOf("testId" to "ID")))
        Mockito.`when`(testReportRepository.findAll()).thenReturn(
            listOf(
                entity("10", "10. открыть форму"),
                entity("2-1", "**Preconditions**:\n1. дочерний кейс"),
                entity("2", "- 1. базовый кейс"),
                entity("ABC", "ручной шаг"),
            ),
        )

        val report = service.getReport()

        assertEquals(listOf("2", "2-1", "10", "ABC"), report.items.map { it.testId }, "Неверная сортировка testId в отчёте")
        assertEquals("базовый кейс", report.items[0].scenario?.steps?.single()?.text, "Legacy scenario должен очищаться от маркера списка и номера")
        assertEquals("дочерний кейс", report.items[1].scenario?.steps?.single()?.text, "Markdown-заголовок Preconditions должен отфильтровываться")
        assertEquals(mapOf("testId" to "ID"), report.translations, "Переводы колонок должны приходить из ColumnConfigService")
    }

    /**
     * Позитивный unit-тест фиксирует batch-upsert во время регресса: статусы нормализуются в enum-name,
     * sync вызывается один раз, а новые записи получают детерминированную readyDate от Clock.
     */
    @Test
    fun `upsert batch during running regression saves normalized rows and syncs run statuses`() {
        Mockito.`when`(testReportRepository.findByTestId(Mockito.anyString())).thenReturn(Optional.empty())
        Mockito.`when`(testReportRepository.save(Mockito.any(TestReportEntity::class.java))).thenAnswer { it.arguments[0] }

        service.upsertBatch(
            TestBatchRequest(
                listOf(
                    TestUpsertItem(" 101 ", " API ", " First ", scenario = scenario("step"), runStatus = " passed "),
                    TestUpsertItem("102", "UI", "Second", scenario = scenario("another"), runStatus = "FAILED"),
                ),
            ),
            isRegressRunning = true,
        )

        val saved = ArgumentCaptor.forClass(TestReportEntity::class.java)
        Mockito.verify(testReportRepository, Mockito.times(2)).save(saved.capture())
        assertEquals(listOf("101", "102"), saved.allValues.map { it.testId }, "testId должны сохраняться trim-нормализованными")
        assertEquals(LocalDate.parse("2026-06-28"), saved.allValues.first().readyDate, "readyDate новой записи должен браться из fixedClock")
        assertEquals(listOf("PASSED", "FAILED"), saved.allValues.map { it.runStatus }, "runStatus должен нормализоваться к enum-name")
        Mockito.verify(regressionService).syncRunningRegressionResults(mapOf("101" to "PASSED", "102" to "FAILED"))
    }

    /**
     * Негативный boundary-тест проверяет обязательность scenario после фильтрации пустых шагов.
     */
    @Test
    fun `upsert rejects scenario when every step is empty after normalization`() {
        Mockito.`when`(testReportRepository.findByTestId("EMPTY-SCENARIO")).thenReturn(Optional.empty())

        assertBadRequest("Required field scenario is missing") {
            service.upsertTest(
                TestUpsertItem("EMPTY-SCENARIO", "API", "Empty", scenario = ScenarioRequest(listOf(ScenarioStepRequest(1, "   ", emptyList())))),
                forceUpdate = false,
            )
        }
        Mockito.verify(testReportRepository, Mockito.never()).save(Mockito.any(TestReportEntity::class.java))
    }

    /**
     * Негативный unit-тест проверяет валидацию ручных полей при forceUpdate: невалидный priority не должен сохраняться.
     */
    @Test
    fun `force update rejects invalid priority and keeps repository unchanged`() {
        Mockito.`when`(testReportRepository.findByTestId("PRIORITY-1")).thenReturn(Optional.empty())

        assertBadRequest("Invalid priority") {
            service.upsertTest(
                TestUpsertItem("PRIORITY-1", "API", "Invalid priority", priority = "Urgent", scenario = scenario("step")),
                forceUpdate = true,
            )
        }
        Mockito.verify(testReportRepository, Mockito.never()).save(Mockito.any(TestReportEntity::class.java))
    }

    /**
     * Edge-тест проверяет delete: отсутствующий testId должен возвращать 404 и не вызывать delete.
     */
    @Test
    fun `delete test returns not found for missing test id`() {
        Mockito.`when`(testReportRepository.findByTestId("MISSING")).thenReturn(Optional.empty())

        assertNotFound("Test with ID MISSING not found") { service.deleteTest("MISSING") }
        Mockito.verify(testReportRepository, Mockito.never()).delete(Mockito.any(TestReportEntity::class.java))
    }

    private fun scenario(text: String) = ScenarioRequest(listOf(ScenarioStepRequest(number = 1, text = text, attachments = emptyList())))

    private fun entity(testId: String, scenario: String) = TestReportEntity(testId = testId).apply {
        category = "API"
        shortTitle = "Title $testId"
        this.scenario = scenario
    }
}
