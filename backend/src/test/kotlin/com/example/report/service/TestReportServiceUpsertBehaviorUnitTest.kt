package com.example.report.service

import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import com.example.report.dto.TestBatchRequest
import com.example.report.dto.TestUpsertItem
import com.example.report.entity.TestReportEntity
import com.example.report.repository.TestReportRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional

class TestReportServiceUpsertBehaviorUnitTest {

    private val testReportRepository: TestReportRepository = Mockito.mock(TestReportRepository::class.java)
    private val columnConfigService: ColumnConfigService = Mockito.mock(ColumnConfigService::class.java)
    private val regressionService: RegressionService = Mockito.mock(RegressionService::class.java)
    private val service = TestReportService(
        testReportRepository,
        columnConfigService,
        regressionService,
        jacksonObjectMapper(),
        fixedClock,
    )

    @Test
    fun `new test without force update ignores manual values and applies every server default`() {
        Mockito.`when`(testReportRepository.findByTestId("CREATE-DEFAULTS")).thenReturn(Optional.empty())
        Mockito.`when`(testReportRepository.save(Mockito.any(TestReportEntity::class.java))).thenAnswer { it.arguments[0] }

        service.upsertTest(
            TestUpsertItem(
                testId = "  CREATE-DEFAULTS  ",
                category = "  API  ",
                shortTitle = "  Defaults  ",
                issueLink = "https://ignored.example/issue",
                readyDate = "2020-01-01",
                generalStatus = "invalid status is ignored",
                priority = "Urgent",
                scenario = scenario("  create  "),
                notes = "ignored notes",
                runStatus = "FAILED",
            ),
            forceUpdate = false,
        )

        val saved = captureSavedEntity()
        assertEquals("CREATE-DEFAULTS", saved.testId)
        assertEquals("API", saved.category)
        assertEquals("Defaults", saved.shortTitle)
        assertEquals("https://youtrackru/issue/", saved.issueLink)
        assertEquals(LocalDate.parse("2026-06-28"), saved.readyDate)
        assertEquals("Готово", saved.generalStatus)
        assertEquals("Medium", saved.priority)
        assertEquals("", saved.notes)
        assertNull(saved.runStatus)
        assertEquals(OffsetDateTime.parse("2026-06-28T10:15:30Z"), saved.updatedAt)
    }

    @Test
    fun `new test with force update normalizes every explicitly supplied manual field`() {
        Mockito.`when`(testReportRepository.findByTestId("CREATE-FORCED")).thenReturn(Optional.empty())
        Mockito.`when`(testReportRepository.save(Mockito.any(TestReportEntity::class.java))).thenAnswer { it.arguments[0] }

        service.upsertTest(
            TestUpsertItem(
                testId = "CREATE-FORCED",
                category = "API",
                shortTitle = "Forced",
                issueLink = "  https://issue/CREATE-FORCED  ",
                readyDate = " 2026-01-15 ",
                generalStatus = "  В работе  ",
                priority = " high ",
                scenario = scenario("forced"),
                notes = "",
                runStatus = "PASSED",
            ),
            forceUpdate = true,
        )

        val saved = captureSavedEntity()
        assertEquals("https://issue/CREATE-FORCED", saved.issueLink)
        assertEquals(LocalDate.parse("2026-01-15"), saved.readyDate)
        assertEquals("В работе", saved.generalStatus)
        assertEquals("High", saved.priority)
        assertEquals("", saved.notes)
        assertNull(saved.runStatus, "Одиночный upsert не должен применять runStatus")
    }

    @Test
    fun `existing test without force update changes required fields and preserves manual invariant fields`() {
        val existing = existingEntity("UPDATE-SAFE")
        Mockito.`when`(testReportRepository.findByTestId("UPDATE-SAFE")).thenReturn(Optional.of(existing))
        Mockito.`when`(testReportRepository.save(existing)).thenReturn(existing)

        service.upsertTest(
            TestUpsertItem(
                testId = "UPDATE-SAFE",
                category = "  New category  ",
                shortTitle = "  New title  ",
                issueLink = "https://ignored/new",
                readyDate = "2030-12-31",
                generalStatus = "invalid but ignored",
                priority = "Urgent",
                scenario = scenario("new scenario"),
                notes = "ignored",
                runStatus = "SKIPPED",
            ),
            forceUpdate = false,
        )

        assertEquals("New category", existing.category)
        assertEquals("New title", existing.shortTitle)
        assertEquals("https://issue/original", existing.issueLink)
        assertEquals(LocalDate.parse("2025-05-20"), existing.readyDate)
        assertEquals("Очередь", existing.generalStatus)
        assertEquals("Blocker", existing.priority)
        assertEquals("original notes", existing.notes)
        assertEquals("PASSED", existing.runStatus)
        assertEquals(OffsetDateTime.parse("2026-06-28T10:15:30Z"), existing.updatedAt)
    }

    @Test
    fun `existing test with force update changes manual fields but keeps run status`() {
        val existing = existingEntity("UPDATE-FORCED")
        Mockito.`when`(testReportRepository.findByTestId("UPDATE-FORCED")).thenReturn(Optional.of(existing))
        Mockito.`when`(testReportRepository.save(existing)).thenReturn(existing)

        service.upsertTest(
            TestUpsertItem(
                testId = "UPDATE-FORCED",
                category = "Updated category",
                shortTitle = "Updated title",
                issueLink = " https://issue/updated ",
                readyDate = "2026-02-03",
                generalStatus = "Неактуально",
                priority = "trivial",
                scenario = scenario("updated"),
                notes = "updated notes",
                runStatus = "FAILED",
            ),
            forceUpdate = true,
        )

        assertEquals("https://issue/updated", existing.issueLink)
        assertEquals(LocalDate.parse("2026-02-03"), existing.readyDate)
        assertEquals("Неактуально", existing.generalStatus)
        assertEquals("Trivial", existing.priority)
        assertEquals("updated notes", existing.notes)
        assertEquals("PASSED", existing.runStatus)
    }

    @Test
    fun `single update falls back to stored required fields when request omits them`() {
        val existing = existingEntity("UPDATE-FALLBACK")
        Mockito.`when`(testReportRepository.findByTestId("UPDATE-FALLBACK")).thenReturn(Optional.of(existing))
        Mockito.`when`(testReportRepository.save(existing)).thenReturn(existing)

        service.upsertTest(TestUpsertItem(testId = "UPDATE-FALLBACK", notes = "changed"), forceUpdate = true)

        assertEquals("Original category", existing.category)
        assertEquals("Original title", existing.shortTitle)
        assertEquals("{\"steps\":[{\"number\":1,\"text\":\"original\",\"attachments\":[]}]}", existing.scenario)
        assertEquals("changed", existing.notes)
    }

    @Test
    fun `blank non nullable manual fields preserve stored values while blank notes clear notes`() {
        val existing = existingEntity("UPDATE-BLANKS")
        Mockito.`when`(testReportRepository.findByTestId("UPDATE-BLANKS")).thenReturn(Optional.of(existing))
        Mockito.`when`(testReportRepository.save(existing)).thenReturn(existing)

        service.upsertTest(
            TestUpsertItem(
                testId = "UPDATE-BLANKS",
                category = "Category",
                shortTitle = "Title",
                issueLink = "   ",
                generalStatus = "   ",
                priority = "   ",
                scenario = scenario("step"),
                notes = "",
            ),
            forceUpdate = true,
        )

        assertEquals("https://issue/original", existing.issueLink)
        assertEquals("Очередь", existing.generalStatus)
        assertEquals("Blocker", existing.priority)
        assertEquals("", existing.notes)
    }

    @Test
    fun `batch does not use stored required fields as fallback`() {
        val existing = existingEntity("BATCH-NO-FALLBACK")
        Mockito.`when`(testReportRepository.findByTestId("BATCH-NO-FALLBACK")).thenReturn(Optional.of(existing))

        assertBadRequest("Required field category is missing") {
            service.upsertBatch(
                TestBatchRequest(listOf(TestUpsertItem(testId = "BATCH-NO-FALLBACK"))),
            )
        }

        Mockito.verify(testReportRepository, Mockito.never()).save(Mockito.any(TestReportEntity::class.java))
    }

    @Test
    fun `batch validates every item before saving the first one`() {
        Mockito.`when`(testReportRepository.findByTestId(Mockito.anyString())).thenReturn(Optional.empty())

        assertBadRequest("Required field shortTitle is missing") {
            service.upsertBatch(
                TestBatchRequest(
                    listOf(
                        TestUpsertItem("BATCH-VALID", "API", "Valid", scenario = scenario("step")),
                        TestUpsertItem("BATCH-INVALID", "API", scenario = scenario("step")),
                    ),
                ),
            )
        }

        Mockito.verify(testReportRepository, Mockito.never()).save(Mockito.any(TestReportEntity::class.java))
        Mockito.verify(regressionService, Mockito.never()).syncRunningRegressionResults(Mockito.anyMap())
    }

    @Test
    fun `running batch requires run status and rejects unsupported value`() {
        Mockito.`when`(testReportRepository.findByTestId(Mockito.anyString())).thenReturn(Optional.empty())
        val requestWithoutStatus = TestBatchRequest(
            listOf(TestUpsertItem("RUN-NONE", "API", "No status", scenario = scenario("step"))),
        )
        val requestWithInvalidStatus = TestBatchRequest(
            listOf(TestUpsertItem("RUN-BAD", "API", "Bad status", scenario = scenario("step"), runStatus = "BROKEN")),
        )

        assertBadRequest("Required field runStatus is missing") {
            service.upsertBatch(requestWithoutStatus, isRegressRunning = true)
        }
        assertBadRequest("Неправильный статус прогона: BROKEN") {
            service.upsertBatch(requestWithInvalidStatus, isRegressRunning = true)
        }

        Mockito.verify(testReportRepository, Mockito.never()).save(Mockito.any(TestReportEntity::class.java))
    }

    @Test
    fun `delete existing test delegates exact entity to repository`() {
        val existing = existingEntity("DELETE-ME")
        Mockito.`when`(testReportRepository.findByTestId("DELETE-ME")).thenReturn(Optional.of(existing))

        service.deleteTest("DELETE-ME")

        Mockito.verify(testReportRepository).delete(existing)
    }

    private fun captureSavedEntity(): TestReportEntity {
        val captor = ArgumentCaptor.forClass(TestReportEntity::class.java)
        Mockito.verify(testReportRepository).save(captor.capture())
        return captor.value
    }

    private fun scenario(text: String) = ScenarioRequest(
        listOf(ScenarioStepRequest(number = 1, text = text, attachments = emptyList())),
    )

    private fun existingEntity(testId: String) = TestReportEntity(testId = testId).apply {
        category = "Original category"
        shortTitle = "Original title"
        issueLink = "https://issue/original"
        readyDate = LocalDate.parse("2025-05-20")
        generalStatus = "Очередь"
        priority = "Blocker"
        scenario = "{\"steps\":[{\"number\":1,\"text\":\"original\",\"attachments\":[]}]}"
        notes = "original notes"
        runStatus = "PASSED"
    }
}
