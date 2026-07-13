package org.golenev.ui.allure

import com.codeborne.selenide.logevents.LogEvent
import com.codeborne.selenide.logevents.LogEventListener
import io.qameta.allure.Allure
import io.qameta.allure.selenide.AllureSelenide

class ReadableAllureSelenideListener : LogEventListener {
    private val delegate = AllureSelenide()
        .includeSelenideSteps(false)
        .screenshots(true)
        .savePageSource(true)

    override fun beforeEvent(event: LogEvent) {
        delegate.beforeEvent(event)
    }

    override fun afterEvent(event: LogEvent) {
        delegate.afterEvent(event)
        buildAttachment(event)?.let { attachment ->
            Allure.addAttachment(attachment.title(), "text/plain", attachment.body())
        }
    }

    internal fun buildAttachment(event: LogEvent): UiEventAttachment? {
        val locator = LocatorNormalizer.normalize(event.element)
        if (locator.isBlank()) return null

        val parsed = UiEventSubjectParser.parse(event.subject) ?: return null
        val error: Throwable? = event.error
        return UiEventAttachment(
            alias = UiElementNameRegistry.findAlias(locator),
            locator = locator,
            eventType = parsed.eventType,
            operation = parsed.operation,
            successCondition = parsed.successCondition,
            because = parsed.because,
            status = event.status.name,
            errorMessage = error?.message,
        )
    }

    private fun UiEventAttachment.title(): String = "UI · $operation · ${alias ?: locator}"

    private fun UiEventAttachment.body(): String = buildString {
        appendLine("Алиас элемента:")
        appendLine(alias.orEmpty())
        appendLine()
        appendLine("Конечный локатор:")
        appendLine(locator)
        appendLine()
        appendLine("Тип операции:")
        appendLine(eventType.name)
        appendLine()
        appendLine("Операция:")
        appendLine(operation)
        appendLine()
        appendLine("Условие успешного выполнения:")
        appendLine(successCondition.orEmpty())
        appendLine()
        appendLine("Ожидаемая причина:")
        appendLine(because.orEmpty())
        appendLine()
        appendLine("Результат:")
        appendLine(status)
        if (!errorMessage.isNullOrBlank()) {
            appendLine()
            appendLine("Ошибка:")
            appendLine(errorMessage)
        }
    }
}
