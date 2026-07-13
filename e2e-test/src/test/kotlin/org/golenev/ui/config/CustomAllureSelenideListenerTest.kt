package org.golenev.ui.config

import com.codeborne.selenide.logevents.LogEvent
import com.codeborne.selenide.logevents.LogEvent.EventStatus.PASS
import io.qameta.allure.Allure
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.AllureResultsWriter
import io.qameta.allure.model.TestResult
import io.qameta.allure.model.TestResultContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.UUID

class CustomAllureSelenideListenerTest {

    private lateinit var writer: CapturingAllureResultsWriter
    private lateinit var lifecycle: AllureLifecycle
    private lateinit var listener: CustomAllureSelenideListener
    private lateinit var testCaseId: String

    @BeforeEach
    fun setUp() {
        UiElementMetadataRegistry.clear()
        writer = CapturingAllureResultsWriter()
        lifecycle = AllureLifecycle(writer)
        Allure.setLifecycle(lifecycle)
        listener = CustomAllureSelenideListener(lifecycle)
        testCaseId = UUID.randomUUID().toString()
        lifecycle.scheduleTestCase(TestResult().setUuid(testCaseId).setName("listener test"))
        lifecycle.startTestCase(testCaseId)
    }

    @AfterEach
    fun tearDown() {
        UiElementMetadataRegistry.clear()
        runCatching {
            lifecycle.stopTestCase(testCaseId)
            lifecycle.writeTestCase(testCaseId)
        }
    }

    @Test
    fun `registry returns last registered locator for alias`() {
        val alias = "Общий alias динамического элемента"

        UiElementMetadataRegistry.register(alias, "[data-id='ID-1']")
        assertEquals("[data-id='ID-1']", UiElementMetadataRegistry.resolveLocator(alias))

        UiElementMetadataRegistry.register(alias, "[data-id='ID-2']")
        assertEquals("[data-id='ID-2']", UiElementMetadataRegistry.resolveLocator(alias))

        UiElementMetadataRegistry.register(alias, "[data-id='ID-3']")
        assertEquals("[data-id='ID-3']", UiElementMetadataRegistry.resolveLocator(alias))
    }

    @Test
    fun `click events with same alias use locator from current registration`() {
        val alias = "Корневой Selenide-элемент строки таблицы, внутри которого ищутся все ячейки и кнопки."
        val locators = listOf(
            "[data-testid='test-report-table']/[data-testid='test-case-row'][data-test-case-id='ID-1']",
            "[data-testid='test-report-table']/[data-testid='test-case-row'][data-test-case-id='ID-2']",
            "[data-testid='test-report-table']/[data-testid='test-case-row'][data-test-case-id='ID-3']",
        )

        locators.forEach { locator ->
            UiElementMetadataRegistry.register(alias, locator)
            publish(SelenideEvent(element = alias, subject = "click", status = PASS))
        }

        val testResult = writtenTest()
        val steps = testResult.steps
        assertEquals(3, steps.size)

        locators.forEachIndexed { index, locator ->
            assertEquals(alias, steps[index].name)
            assertTrue(stepAttachmentText(testResult, index).contains(locator))
        }
    }

    @Test
    fun `scroll events are not element interactions`() {
        UiElementMetadataRegistry.register("Прокручиваемый элемент", ".scrollable")

        publish(SelenideEvent(element = "Прокручиваемый элемент", subject = "scroll to", status = PASS))
        publish(SelenideEvent(element = "Прокручиваемый элемент", subject = "scroll into view", status = PASS))

        assertTrue(writtenTest().steps.isEmpty())
    }

    @Test
    fun `supported interaction events create steps`() {
        val actions = listOf(
            "click",
            "context click",
            "double click",
            "clear",
            "hover",
            "send keys",
            "set selected",
            "set value",
            "submit",
            "type",
            "unfocus",
        )

        actions.forEach { action ->
            publish(SelenideEvent(element = ".tracked", subject = action, status = PASS))
        }

        assertEquals(actions.size, writtenTest().steps.size)
    }

    @Test
    fun `explicit condition with different locator is not attached to interaction`() {
        val alias = "Одинаковый alias для разных элементов"

        UiElementMetadataRegistry.register(alias, ".first")
        publish(SelenideEvent(element = alias, subject = "should be(visible)", status = PASS))

        UiElementMetadataRegistry.register(alias, ".second")
        publish(SelenideEvent(element = alias, subject = "click", status = PASS))

        val attachmentText = stepAttachmentText(writtenTest(), 0)
        assertTrue(attachmentText.contains(".second"))
        assertFalse(attachmentText.contains("Явное:"))
    }

    private fun publish(event: LogEvent) {
        listener.beforeEvent(event)
        listener.afterEvent(event)
    }

    private fun writtenTest(): TestResult {
        lifecycle.stopTestCase(testCaseId)
        lifecycle.writeTestCase(testCaseId)
        return writer.testResults.last()
    }

    private fun stepAttachmentText(testResult: TestResult, stepIndex: Int): String {
        val source = testResult.steps[stepIndex].attachments.single().source
        return writer.attachments.getValue(source).decodeToString()
    }

    private data class SelenideEvent(
        private val element: String,
        private val subject: String,
        private val status: LogEvent.EventStatus,
        private val error: Throwable? = null,
    ) : LogEvent {

        override fun getElement(): String = element

        override fun getSubject(): String = subject

        override fun getStatus(): LogEvent.EventStatus = status

        override fun getDuration(): Long = 0

        override fun getStartTime(): Long = 0

        override fun getEndTime(): Long = 0

        override fun getError(): Throwable? = error
    }

    private class CapturingAllureResultsWriter : AllureResultsWriter {

        val testResults = mutableListOf<TestResult>()
        val attachments = linkedMapOf<String, ByteArray>()

        override fun write(testResult: TestResult) {
            testResults += testResult
        }

        override fun write(testResultContainer: TestResultContainer) = Unit

        override fun write(source: String, attachment: InputStream) {
            attachments[source] = attachment.readBytes()
        }
    }
}
