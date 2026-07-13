package org.golenev.ui.allure

import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.logevents.LogEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReadableAllureSelenideListenerTest {
    private val listener = ReadableAllureSelenideListener()

    @AfterEach
    fun clearRegistry() {
        UiElementNameRegistry.clear()
    }

    @Test
    fun `element name registers locator to alias and returns same object without replacing locator`() {
        val element = `$`("[data-testid='page-title']")
        val namedElement = element.name(" Заголовок страницы ")

        assertSame(element, namedElement)
        assertEquals("Заголовок страницы", UiElementNameRegistry.findAlias("$([data-testid='page-title'])"))
        assertTrue(namedElement.getSearchCriteria().contains("[data-testid='page-title']"))
        assertFalse(namedElement.getSearchCriteria().contains("Заголовок страницы"))
    }

    @Test
    fun `collection name registers alias found by event locator`() {
        val collection = `$$`("[data-testid='test-report-table'] [data-testid='test-case-row']:not([data-state='draft'])")
        assertSame(collection, collection.name("Сохранённые строки тест-кейсов"))

        val attachment = listener.buildAttachment(
            event(
                element = "[data-testid='test-report-table'] [data-testid='test-case-row']:not([data-state='draft'])",
                subject = "should have(size(2))",
            )
        )

        assertEquals("Сохранённые строки тест-кейсов", attachment?.alias)
    }

    @Test
    fun `same alias is allowed for different locators`() {
        UiElementNameRegistry.register("[data-test-case-id='ID-1']", "Строка тест-кейса")
        UiElementNameRegistry.register("[data-test-case-id='ID-2']", "Строка тест-кейса")

        assertEquals("Строка тест-кейса", UiElementNameRegistry.findAlias("[data-test-case-id='ID-1']"))
        assertEquals("Строка тест-кейса", UiElementNameRegistry.findAlias("[data-test-case-id='ID-2']"))
    }

    @Test
    fun `dynamic locators resolve aliases independently`() {
        listOf("ID-1", "ID-2", "ID-3").forEach { id ->
            UiElementNameRegistry.register("[data-testid='test-case-row'][data-test-case-id='$id']", "Строка тест-кейса $id")
        }

        assertEquals("Строка тест-кейса ID-1", UiElementNameRegistry.findAlias("$([data-testid='test-case-row'][data-test-case-id='ID-1'])"))
        assertEquals("Строка тест-кейса ID-2", UiElementNameRegistry.findAlias("By.cssSelector: [data-testid='test-case-row'][data-test-case-id='ID-2']"))
        assertEquals("Строка тест-кейса ID-3", UiElementNameRegistry.findAlias("[data-testid='test-case-row'][data-test-case-id='ID-3']"))
    }

    @Test
    fun `parses should have subject`() {
        val parsed = UiEventSubjectParser.parse("should have(size(2))")

        assertEquals(UiEventType.CHECK, parsed?.eventType)
        assertEquals("should have", parsed?.operation)
        assertEquals("size(2)", parsed?.successCondition)
        assertNull(parsed?.because)
    }

    @Test
    fun `parses should be subject with because`() {
        val parsed = UiEventSubjectParser.parse("should be(visible (because текст причины))")

        assertEquals("should be", parsed?.operation)
        assertEquals("visible", parsed?.successCondition)
        assertEquals("текст причины", parsed?.because)
    }

    @Test
    fun `parses because with parentheses inside text`() {
        val parsed = UiEventSubjectParser.parse("should be(visible (because текст причины (с деталями) перед кликом))")

        assertEquals("visible", parsed?.successCondition)
        assertEquals("текст причины (с деталями) перед кликом", parsed?.because)
    }

    @Test
    fun `shouldBe and click create two independent attachments`() {
        UiElementNameRegistry.register(".regression-actions .danger-btn", "Кнопка остановки regression run")

        val shouldAttachment = listener.buildAttachment(event("$(\".regression-actions .danger-btn\")", "should be(visible (because кнопка должна отображаться))"))
        val clickAttachment = listener.buildAttachment(event("$(\".regression-actions .danger-btn\")", "click"))

        assertEquals(UiEventType.CHECK, shouldAttachment?.eventType)
        assertEquals("visible", shouldAttachment?.successCondition)
        assertEquals(UiEventType.ACTION, clickAttachment?.eventType)
        assertEquals("clickable: interactable и enabled", clickAttachment?.successCondition)
        assertEquals("Кнопка остановки regression run", clickAttachment?.alias)
    }

    @Test
    fun `scroll events are ignored`() {
        assertNull(listener.buildAttachment(event("[data-testid='row']", "scroll to")))
        assertNull(listener.buildAttachment(event("[data-testid='row']", "scroll into view")))
    }

    @Test
    fun `only required actions are supported`() {
        val supported = listOf("clear", "click", "context click", "double click", "hover", "send keys(A)", "set selected(true)", "set value(test value)", "submit", "type(text)", "unfocus")
        supported.forEach { subject -> assertNotNull(UiEventSubjectParser.parse(subject), subject) }

        val ignored = listOf("find", "findAll", "$", "$$", "getText", "text", "attr", "attribute", "exists", "isDisplayed")
        ignored.forEach { subject -> assertNull(UiEventSubjectParser.parse(subject), subject) }
    }

    @Test
    fun `failed event contains error message`() {
        val attachment = listener.buildAttachment(
            event("[data-testid='row']", "should be(visible)", LogEvent.EventStatus.FAIL, AssertionError("not visible"))
        )

        assertEquals("FAIL", attachment?.status)
        assertEquals("not visible", attachment?.errorMessage)
    }

    @Test
    fun `registry is cleared between tests`() {
        UiElementNameRegistry.register("[data-testid='row']", "Строка")
        UiElementNameRegistry.clear()

        assertNull(UiElementNameRegistry.findAlias("[data-testid='row']"))
    }

    private fun event(
        element: String,
        subject: String,
        status: LogEvent.EventStatus = LogEvent.EventStatus.PASS,
        error: Throwable? = null,
    ): LogEvent = object : LogEvent {
        override fun getElement(): String = element
        override fun getSubject(): String = subject
        override fun getStatus(): LogEvent.EventStatus = status
        override fun getDuration(): Long = 1
        override fun getStartTime(): Long = 1
        override fun getEndTime(): Long = 2
        override fun getError(): Throwable = error ?: AssertionError("")
    }
}
