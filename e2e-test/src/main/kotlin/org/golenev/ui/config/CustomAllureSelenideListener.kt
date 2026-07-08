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
import org.golenev.ui.pages.REPORT_LOCATOR_SEPARATOR
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.concurrent.getOrSet

class CustomAllureSelenideListener : LogEventListener {

    private val lifecycle: AllureLifecycle = Allure.getLifecycle()

    private val stepIds = ThreadLocal<MutableList<String>>()

    private val pendingElementConditions = ThreadLocal<MutableMap<String, ElementConditionDetails>>()

    private val aliasLocators: Map<String, String> by lazy { collectAliasLocators() }

    private val elementInteractionSubjects = setOf(
        "click",
        "double click",
        "context click",
        "set value",
        "append",
        "send keys",
        "type",
        "select option",
        "scroll into view",
        "hover",
    )

    override fun beforeEvent(event: LogEvent) {
        if (!event.isReportableElementEvent()) return

        val stepId = UUID.randomUUID().toString()

        stepIds.getOrSet { mutableListOf() }.add(stepId)

        lifecycle.startStep(
            stepId,
            StepResult().setName(event.readableStepName())
        )
    }

    override fun afterEvent(event: LogEvent) {
        if (!event.isReportableElementEvent()) return

        attachReadableSelenideInfo(event)

        if (event.isElementCondition()) {
            rememberElementCondition(event)
        }

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

            val conditionDetails = event.conditionDetails()

            appendLine("Условия для успешного взаимодействия:")
            appendLine(conditionDetails.condition)
            appendLine()

            appendLine("Ожидаемая причина:")
            appendLine(conditionDetails.because)
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

    private fun LogEvent.isReportableElementEvent(): Boolean = isElementInteraction() || isElementCondition()

    private fun LogEvent.isElementInteraction(): Boolean {
        if (element.isBlank()) return false

        val interaction = subject.removeBecauseBlock()

        return elementInteractionSubjects.any { interaction.startsWith(it) }
    }

    private fun LogEvent.isElementCondition(): Boolean {
        if (element.isBlank()) return false

        val condition = subject.removeBecauseBlock()

        return condition.startsWith("should be") ||
            condition.startsWith("should have") ||
            condition.startsWith("should")
    }

    private fun rememberElementCondition(event: LogEvent) {
        pendingElementConditions.getOrSet { mutableMapOf() }[event.element] = ElementConditionDetails(
            condition = event.subject.extractCondition(),
            because = event.subject.extractBecause(),
        )
    }

    private fun LogEvent.conditionDetails(): ElementConditionDetails {
        if (isElementCondition()) {
            return ElementConditionDetails(
                condition = subject.extractCondition(),
                because = subject.extractBecause(),
            )
        }

        return pendingElementConditions.getOrSet { mutableMapOf() }.remove(element)
            ?: ElementConditionDetails(
                condition = subject.extractCondition(),
                because = subject.extractBecause(),
            )
    }

    private fun LogEvent.toAllureStatus(): Status {
        return when (status) {
            PASS -> Status.PASSED
            FAIL -> Status.FAILED
            else -> Status.BROKEN
        }
    }

    private fun String.extractAlias(): String? {
        val value = trim().substringBefore(REPORT_LOCATOR_SEPARATOR)

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
        val value = trim()
        if (REPORT_LOCATOR_SEPARATOR in value) {
            return value.substringAfter(REPORT_LOCATOR_SEPARATOR).ifBlank { "Локатор не определён" }
        }

        val alias = extractAlias()
        if (alias != null) {
            aliasLocators[alias]?.let { return it }
        }

        return ifBlank { "Локатор не определён" }
    }

    private fun String.extractCondition(): String {
        return removeBecauseBlock()
            .normalizeSelenideCondition()
            .ifBlank { "Условие не определено" }
    }

    private fun String.extractBecause(): String {
        val becauseRegex = Regex("""\(because\s+(.+?)\)+\s*$""")

        return becauseRegex.find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.ifBlank { null }
            ?: "Because не указан"
    }

    private fun String.removeBecauseBlock(): String {
        return replace(Regex("""\s*\(because\s+.+?\)+\s*$"""), "")
            .trim()
    }

    private fun String.normalizeSelenideCondition(): String {
        val value = trim()
        val conditionWithArguments = Regex("""^(should(?:\s+\w+)*)\((.+)\)?$""")

        return conditionWithArguments.matchEntire(value)
            ?.let { match -> "${match.groupValues[1]} ${match.groupValues[2].trimEnd(')')}" }
            ?: value
    }

    private fun collectAliasLocators(): Map<String, String> {
        val sourceRoot = listOf(
            Path.of("src/main/kotlin"),
            Path.of("e2e-test/src/main/kotlin"),
        ).firstOrNull { Files.isDirectory(it) } ?: return emptyMap()

        return Files.walk(sourceRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .map { Files.readString(it) }
                .flatMap { source ->
                    aliasLocatorRegex.findAll(source)
                        .map { it.groupValues[2] to it.groupValues[1] }
                        .toList()
                        .stream()
                }
                .toList()
                .toMap()
        }
    }

    private data class ElementConditionDetails(
        val condition: String,
        val because: String,
    )

    private companion object {
        private val aliasLocatorRegex = Regex("""`?${'$'}`?\("([^"]+)"\)\.`as`\("([^"]+)"\)""")
    }
}
