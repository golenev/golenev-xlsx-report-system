package com.example.report.service

import com.example.report.config.ColumnConfigProperties
import com.example.report.dto.ScenarioAttachmentRequest
import com.example.report.dto.ScenarioParameterRequest
import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import com.example.report.dto.TestUpsertItem
import com.example.report.entity.TestReportEntity
import com.example.report.repository.TestReportRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.util.Optional

class TestReportScenarioUnitTest {

    private val testReportRepository: TestReportRepository = Mockito.mock(TestReportRepository::class.java)
    private val columnConfigService: ColumnConfigService = Mockito.mock(ColumnConfigService::class.java)
    private val regressionService: RegressionService = Mockito.mock(RegressionService::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val service = TestReportService(
        testReportRepository,
        columnConfigService,
        regressionService,
        objectMapper,
        fixedClock,
    )

    @Test
    fun `structured scenario recursively normalizes metadata and removes negative measurements`() {
        Mockito.`when`(testReportRepository.findByTestId("SCENARIO-METADATA")).thenReturn(Optional.empty())
        Mockito.`when`(testReportRepository.save(Mockito.any(TestReportEntity::class.java))).thenAnswer { it.arguments[0] }

        service.upsertTest(
            TestUpsertItem(
                testId = "SCENARIO-METADATA",
                category = "API",
                shortTitle = "Metadata",
                scenario = ScenarioRequest(
                    steps = listOf(
                        ScenarioStepRequest(
                            number = 7,
                            text = "  root step  ",
                            attachments = listOf(
                                ScenarioAttachmentRequest(
                                    name = "  request  ",
                                    mediaType = "  application/json  ",
                                    content = "  body is not trimmed  ",
                                    source = "source.txt",
                                    sizeBytes = -1,
                                ),
                            ),
                            subSteps = listOf(
                                ScenarioStepRequest(
                                    number = 3,
                                    text = "  child  ",
                                    attachments = emptyList(),
                                    durationMs = 15,
                                    parameters = listOf(ScenarioParameterRequest("  expected  ", " value ")),
                                ),
                            ),
                            durationMs = -5,
                        ),
                    ),
                ),
            ),
            forceUpdate = false,
        )

        val captor = ArgumentCaptor.forClass(TestReportEntity::class.java)
        Mockito.verify(testReportRepository).save(captor.capture())
        val stored = objectMapper.readValue(captor.value.scenario, ScenarioRequest::class.java)
        val root = stored.steps.single()
        val attachment = root.attachments.orEmpty().single()
        val child = root.subSteps.single()

        assertEquals(7, root.number)
        assertEquals("root step", root.text)
        assertNull(root.durationMs)
        assertEquals("request", attachment.name)
        assertEquals("application/json", attachment.mediaType)
        assertEquals("  body is not trimmed  ", attachment.content)
        assertEquals("source.txt", attachment.source)
        assertNull(attachment.sizeBytes)
        assertEquals(15, child.durationMs)
        assertEquals("expected", child.parameters.single().name)
        assertEquals(" value ", child.parameters.single().value)
    }

    @Test
    fun `scenario step requires number text and attachments independently`() {
        Mockito.`when`(testReportRepository.findByTestId(Mockito.anyString())).thenReturn(Optional.empty())

        assertBadRequest("Field scenario.steps.number is required") {
            upsertScenario(ScenarioStepRequest(text = "step", attachments = emptyList()))
        }
        assertBadRequest("Field scenario.steps.text is required") {
            upsertScenario(ScenarioStepRequest(number = 1, attachments = emptyList()))
        }
        assertBadRequest("Field scenario.steps.attachments must be an array") {
            upsertScenario(ScenarioStepRequest(number = 1, text = "step", attachments = null))
        }

        Mockito.verify(testReportRepository, Mockito.never()).save(Mockito.any(TestReportEntity::class.java))
    }

    @Test
    fun `legacy text parser preserves hierarchy multiline text and fenced attachment`() {
        val parsed = service.buildScenarioFromText(
            """
                **Preconditions**:
                1. Root step
                continuation
                  1.1 Child step
                ```json
                {"ok":true}
                ```
                Шаги не найдены
            """.trimIndent(),
        )

        val root = parsed.steps.single()
        val child = root.subSteps.single()
        assertEquals("Root step\ncontinuation", root.text)
        assertEquals("Child step", child.text)
        assertEquals("{\"ok\":true}", child.attachments.orEmpty().single().content)
        assertEquals("Attachment", child.attachments.orEmpty().single().name)
        assertEquals("text/plain", child.attachments.orEmpty().single().mediaType)
    }

    @Test
    fun `report repairs legacy flat json attachment steps and keeps only business step`() {
        val storedScenario = """
            {
              "steps": [
                {"number":1,"text":"Business step","attachments":[]},
                {"number":2,"text":"```json","attachments":[]},
                {"number":3,"text":"{\"ok\":true}","attachments":[]},
                {"number":4,"text":"```","attachments":[]},
                {"number":5,"text":"Attachment: response","attachments":[{"content":"HTTP 200"}]}
              ]
            }
        """.trimIndent()
        Mockito.`when`(testReportRepository.findAll()).thenReturn(listOf(entity("LEGACY-FLAT", storedScenario)))
        Mockito.`when`(columnConfigService.getConfig()).thenReturn(ColumnConfigProperties(emptyMap()))

        val scenario = service.getReport().items.single().scenario

        val step = scenario?.steps?.single()
        assertEquals("Business step", step?.text)
        assertEquals(
            "{\"ok\":true}\nAttachment: response\nHTTP 200",
            step?.attachments.orEmpty().single().content,
        )
    }

    @Test
    fun `malformed json scenario falls back to one legacy text step`() {
        Mockito.`when`(testReportRepository.findAll()).thenReturn(listOf(entity("BROKEN-JSON", "{not-json")))
        Mockito.`when`(columnConfigService.getConfig()).thenReturn(ColumnConfigProperties(emptyMap()))

        val scenario = service.getReport().items.single().scenario

        assertEquals("{not-json", scenario?.steps?.single()?.text)
        assertEquals(emptyList<ScenarioAttachmentRequest>(), scenario?.steps?.single()?.attachments)
    }

    private fun upsertScenario(step: ScenarioStepRequest) {
        service.upsertTest(
            TestUpsertItem(
                testId = "SCENARIO-INVALID",
                category = "API",
                shortTitle = "Invalid scenario",
                scenario = ScenarioRequest(listOf(step)),
            ),
            forceUpdate = false,
        )
    }

    private fun entity(testId: String, scenario: String) = TestReportEntity(testId = testId).apply {
        category = "API"
        shortTitle = "Title"
        this.scenario = scenario
    }
}
