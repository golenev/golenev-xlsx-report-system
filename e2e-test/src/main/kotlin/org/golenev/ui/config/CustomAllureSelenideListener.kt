package org.golenev.ui.config

import com.codeborne.selenide.Screenshots
import com.codeborne.selenide.WebDriverRunner
import com.codeborne.selenide.logevents.LogEvent
import com.codeborne.selenide.logevents.LogEvent.EventStatus.FAIL
import com.codeborne.selenide.logevents.LogEvent.EventStatus.PASS
import com.codeborne.selenide.logevents.LogEventListener
import io.qameta.allure.Allure
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.model.StepResult
import java.nio.file.Files
import java.util.*
import kotlin.concurrent.getOrSet

class CustomAllureSelenideListener : LogEventListener {

    private val lifecycle: AllureLifecycle = Allure.getLifecycle()

    private val stepIds = ThreadLocal<MutableList<String>>()

    private val elementInteractionSubjects = setOf(
        "click",
        "double click",
        "context click",
        "set value",
        "append",
        "send keys",
        "type",
        "hover",
    )

    override fun beforeEvent(event: LogEvent) {
        if (!event.isElementInteraction()) return

        val stepId = UUID.randomUUID().toString()

        stepIds.getOrSet { mutableListOf() }.add(stepId)

        lifecycle.startStep(
            stepId,
            StepResult().setName(event.readableStepName())
        )
    }

    override fun afterEvent(event: LogEvent) {
        if (!event.isElementInteraction()) return

        attachReadableSelenideInfo(event)

        if (event.status == FAIL) {
            attachScreenshot()
            attachPageSource()
        }

        val stepId = stepIds.getOrSet { mutableListOf() }.removeLastOrNull()
            ?: return

        lifecycle.updateStep(stepId) { step ->
            step.status = event.toAllureStatus()
            step.statusDetails = event.error?.let { error ->
                StatusDetails()
                    .setMessage(error.message)
                    .setTrace(error.stackTraceToString())
            }
        }

        lifecycle.stopStep(stepId)
    }

    private fun LogEvent.readableStepName(): String {
        val alias = element.extractAlias()

        return if (alias != null) {
            alias
        } else {
            element.extractLocator()
        }
    }

    private fun attachReadableSelenideInfo(event: LogEvent) {
        val attachmentText = buildString {
            appendLine("Алиас элемента:")
            appendLine(event.element.extractAlias() ?: "Алиас не определён")
            appendLine()

            appendLine("Конечный локатор:")
            appendLine(event.element.extractLocator())
            appendLine()

            appendLine("Условия для успешного взаимодействия:")
            appendLine(event.subject.extractCondition())
            appendLine()

            appendLine("Ожидаемая причина:")
            appendLine(event.subject.extractBecause())
        }

        Allure.addAttachment(
            "Детали UI-взаимодействия",
            "text/plain",
            attachmentText,
        )
    }

    private fun attachScreenshot() {
        val screenshot = Screenshots.takeScreenShotAsFile() ?: return

        Files.newInputStream(screenshot.toPath()).use { inputStream ->
            Allure.addAttachment(
                "Screenshot",
                "image/png",
                inputStream,
                ".png",
            )
        }
    }

    private fun attachPageSource() {
        val pageSource = runCatching {
            WebDriverRunner.source()
        }.getOrNull() ?: return

        Allure.addAttachment(
            "Page source",
            "text/html",
            pageSource,
            ".html",
        )
    }

    private fun LogEvent.isElementInteraction(): Boolean {
        if (element.isBlank()) return false

        val interaction = subject.removeBecauseBlock()

        return elementInteractionSubjects.any { interaction.startsWith(it) }
    }

    private fun LogEvent.toAllureStatus(): Status {
        return when (status) {
            PASS -> Status.PASSED
            FAIL -> Status.FAILED
            else -> Status.BROKEN
        }
    }

    private fun String.extractAlias(): String? {
        val value = trim()

        if (value.isBlank()) return null

        return when {
            value.startsWith("$(") -> null
            value.startsWith("By.") -> null
            value.startsWith("{By.") -> null
            value.contains("By.xpath") -> null
            value.contains("By.cssSelector") -> null
            else -> value
        }
    }

    private fun String.extractLocator(): String {
        return ifBlank { "Локатор не определён" }
    }

    private fun String.extractCondition(): String {
        return removeBecauseBlock()
            .ifBlank { "Условие не определено" }
    }

    private fun String.extractBecause(): String {
        val becauseRegex = Regex("""\(because\s+(.+?)\)\s*$""")

        return becauseRegex.find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.ifBlank { null }
            ?: "Because не указан"
    }

    private fun String.removeBecauseBlock(): String {
        return replace(Regex("""\s*\(because\s+.+?\)\s*$"""), "")
            .trim()
    }
}
