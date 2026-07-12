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
import java.util.IdentityHashMap
import java.util.UUID

class CustomAllureSelenideListener(
    private val lifecycle: AllureLifecycle = Allure.getLifecycle(),
) : LogEventListener {

    private val state = ThreadLocal.withInitial {
        ListenerState()
    }

    override fun beforeEvent(event: LogEvent) {
        val currentState = state.get()

        when {
            event.isConditionEvent() -> Unit

            event.isElementInteraction() -> {
                val conditions = currentState.pendingConditions.toList()

                currentState.pendingConditions.clear()

                val interactionDetails = event.toInteractionDetails(conditions)
                val stepId = UUID.randomUUID().toString()

                currentState.runningSteps[event] = RunningStep(
                    stepId = stepId,
                    interactionDetails = interactionDetails,
                )

                lifecycle.startStep(
                    stepId,
                    StepResult().setName(
                        interactionDetails.alias
                            ?: interactionDetails.locator
                            ?: event.element
                    )
                )
            }

            !event.isMetadataEventBetweenConditionAndInteraction() -> {
                currentState.pendingConditions.clear()
            }
        }
    }

    override fun afterEvent(event: LogEvent) {
        val currentState = state.get()

        when {
            event.isConditionEvent() -> {
                handleConditionEvent(
                    event = event,
                    currentState = currentState,
                )
            }

            event.isElementInteraction() -> {
                handleInteractionEvent(
                    event = event,
                    currentState = currentState,
                )
            }
        }

        if (
            currentState.pendingConditions.isEmpty() &&
            currentState.runningSteps.isEmpty()
        ) {
            state.remove()
        }
    }

    private fun handleConditionEvent(
        event: LogEvent,
        currentState: ListenerState,
    ) {
        val conditionDetails = event.toConditionDetails()

        if (event.status == PASS) {
            val previousElement = currentState.pendingConditions
                .lastOrNull()
                ?.elementDescription

            if (
                previousElement != null &&
                previousElement != conditionDetails.elementDescription
            ) {
                currentState.pendingConditions.clear()
            }

            currentState.pendingConditions += conditionDetails
            return
        }

        currentState.pendingConditions.clear()

        if (event.status == FAIL) {
            finishFailedConditionStep(
                event = event,
                interactionDetails = InteractionDetails(
                    alias = conditionDetails.alias,
                    locator = conditionDetails.locator,
                    conditions = listOf(conditionDetails),
                ),
            )
        }
    }

    private fun handleInteractionEvent(
        event: LogEvent,
        currentState: ListenerState,
    ) {
        val runningStep = currentState.runningSteps.remove(event)
            ?: return

        attachReadableSelenideInfo(
            interactionDetails = runningStep.interactionDetails,
        )

        if (event.status == FAIL) {
            attachScreenshot()
            attachPageSource()
        }

        lifecycle.updateStep(runningStep.stepId) { step ->
            step.status = event.toAllureStatus()
            step.statusDetails = event.error.toStatusDetails()
        }

        lifecycle.stopStep(runningStep.stepId)
    }

    private fun finishFailedConditionStep(
        event: LogEvent,
        interactionDetails: InteractionDetails,
    ) {
        val stepId = UUID.randomUUID().toString()

        lifecycle.startStep(
            stepId,
            StepResult().setName(
                interactionDetails.alias
                    ?: interactionDetails.locator
                    ?: event.element
            )
        )

        attachReadableSelenideInfo(interactionDetails)
        attachScreenshot()
        attachPageSource()

        lifecycle.updateStep(stepId) { step ->
            step.status = Status.FAILED
            step.statusDetails = event.error.toStatusDetails()
        }

        lifecycle.stopStep(stepId)
    }

    private fun LogEvent.toInteractionDetails(
        conditions: List<ConditionDetails>,
    ): InteractionDetails {
        val locatorFromCondition = conditions
            .lastOrNull { it.locator != null }
            ?.locator

        val aliasFromCondition = conditions
            .lastOrNull { it.alias != null }
            ?.alias

        val elementIsLocator = element.looksLikeLocator()

        return InteractionDetails(
            alias = when {
                !elementIsLocator -> element
                aliasFromCondition != null -> aliasFromCondition
                else -> null
            },
            locator = locatorFromCondition
                ?: element.takeIf { elementIsLocator },
            conditions = conditions,
        )
    }

    private fun LogEvent.toConditionDetails(): ConditionDetails {
        val parsedCondition = subject.parseCondition()
        val elementIsLocator = element.looksLikeLocator()

        return ConditionDetails(
            elementDescription = element,
            alias = element.takeUnless { elementIsLocator },
            locator = element.takeIf { elementIsLocator },
            condition = parsedCondition.condition,
            because = parsedCondition.because,
        )
    }

    private fun attachReadableSelenideInfo(
        interactionDetails: InteractionDetails,
    ) {
        val conditions = interactionDetails.conditions
            .map { it.condition }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty {
                listOf("Явное условие should* перед воздействием не указано")
            }
            .joinToString(System.lineSeparator())

        val because = interactionDetails.conditions
            .mapNotNull { it.because }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty {
                listOf("Because не указан")
            }
            .joinToString(System.lineSeparator())

        val attachmentText = buildString {
            appendLine("Алиас элемента:")
            appendLine(
                interactionDetails.alias
                    ?: "Алиас не определён"
            )
            appendLine()

            appendLine("Конечный локатор:")
            appendLine(
                interactionDetails.locator
                    ?: "Локатор недоступен: перед воздействием не было явного should*"
            )
            appendLine()

            appendLine("Условия для успешного взаимодействия:")
            appendLine(conditions)
            appendLine()

            appendLine("Ожидаемая причина:")
            appendLine(because)
        }

        Allure.addAttachment(
            "Детали UI-взаимодействия",
            "text/plain",
            attachmentText,
        )
    }

    private fun attachScreenshot() {
        runCatching {
            val screenshot = Screenshots.takeScreenShotAsFile()
                ?: return@runCatching

            Files.newInputStream(screenshot.toPath()).use { inputStream ->
                Allure.addAttachment(
                    "Screenshot",
                    "image/png",
                    inputStream,
                    ".png",
                )
            }
        }
    }

    private fun attachPageSource() {
        runCatching {
            if (!WebDriverRunner.hasWebDriverStarted()) {
                return@runCatching
            }

            Allure.addAttachment(
                "Page source",
                "text/html",
                WebDriverRunner.source(),
                ".html",
            )
        }
    }

    private fun LogEvent.isConditionEvent(): Boolean {
        return subject.methodName() in CONDITION_METHODS
    }

    private fun LogEvent.isElementInteraction(): Boolean {
        if (element.isBlank()) {
            return false
        }

        val methodName = subject.methodName()

        return methodName in ELEMENT_INTERACTION_METHODS ||
            methodName == "val" && subject != "val()"
    }

    private fun LogEvent.isMetadataEventBetweenConditionAndInteraction(): Boolean {
        return subject.methodName() in METADATA_METHODS
    }

    private fun String.methodName(): String {
        return substringBefore("(").trim()
    }

    private fun String.parseCondition(): ParsedCondition {
        val methodName = methodName()

        val arguments = substringAfter(
            delimiter = "(",
            missingDelimiterValue = "",
        )
            .removeSuffix(")")
            .trim()

        val conditionWithoutBecause = arguments.extractBecauseBlocks()

        val conditionArguments = conditionWithoutBecause.value
            .trim()
            .removeSurrounding(
                prefix = "[",
                suffix = "]",
            )
            .trim()

        return ParsedCondition(
            condition = listOf(
                methodName,
                conditionArguments,
            )
                .filter { it.isNotBlank() }
                .joinToString(" "),
            because = conditionWithoutBecause.because
                .filter { it.isNotBlank() }
                .distinct()
                .takeIf { it.isNotEmpty() }
                ?.joinToString(System.lineSeparator()),
        )
    }

    private fun String.extractBecauseBlocks(): ExtractedBecause {
        val valueWithoutBecause = StringBuilder()
        val becauseValues = mutableListOf<String>()

        var currentIndex = 0

        while (currentIndex < length) {
            val becauseStart = indexOf(
                string = BECAUSE_PREFIX,
                startIndex = currentIndex,
            )

            if (becauseStart == -1) {
                valueWithoutBecause.append(
                    substring(currentIndex)
                )
                break
            }

            valueWithoutBecause.append(
                substring(
                    startIndex = currentIndex,
                    endIndex = becauseStart,
                )
            )

            val becauseValueStart = becauseStart + BECAUSE_PREFIX.length

            var cursor = becauseValueStart
            var parenthesesDepth = 1

            while (
                cursor < length &&
                parenthesesDepth > 0
            ) {
                when (this[cursor]) {
                    '(' -> parenthesesDepth++
                    ')' -> parenthesesDepth--
                }

                cursor++
            }

            if (parenthesesDepth != 0) {
                valueWithoutBecause.append(
                    substring(becauseStart)
                )
                break
            }

            becauseValues += substring(
                startIndex = becauseValueStart,
                endIndex = cursor - 1,
            ).trim()

            currentIndex = cursor
        }

        return ExtractedBecause(
            value = valueWithoutBecause.toString(),
            because = becauseValues,
        )
    }

    private fun String.looksLikeLocator(): Boolean {
        val value = trim()

        return value.startsWith("$(") ||
            value.startsWith("By.") ||
            value.startsWith("{By.") ||
            value.startsWith("{") ||
            value.startsWith("[") ||
            value.startsWith("#") ||
            value.startsWith(".") ||
            value.startsWith("//") ||
            value.startsWith("/html") ||
            value.contains("By.xpath") ||
            value.contains("By.cssSelector")
    }

    private fun LogEvent.toAllureStatus(): Status {
        return when (status) {
            PASS -> Status.PASSED
            FAIL -> Status.FAILED
            else -> Status.BROKEN
        }
    }

    private fun Throwable.toStatusDetails(): StatusDetails {
        return StatusDetails()
            .setMessage(message)
            .setTrace(stackTraceToString())
    }

    private data class ListenerState(
        val pendingConditions: MutableList<ConditionDetails> = mutableListOf(),
        val runningSteps: IdentityHashMap<LogEvent, RunningStep> =
            IdentityHashMap(),
    )

    private data class RunningStep(
        val stepId: String,
        val interactionDetails: InteractionDetails,
    )

    private data class InteractionDetails(
        val alias: String?,
        val locator: String?,
        val conditions: List<ConditionDetails>,
    )

    private data class ConditionDetails(
        val elementDescription: String,
        val alias: String?,
        val locator: String?,
        val condition: String,
        val because: String?,
    )

    private data class ParsedCondition(
        val condition: String,
        val because: String?,
    )

    private data class ExtractedBecause(
        val value: String,
        val because: List<String>,
    )

    private companion object {

        const val BECAUSE_PREFIX = " (because "

        val CONDITION_METHODS = setOf(
            "should",
            "should be",
            "should have",
            "should not",
            "should not be",
            "should not have",
        )

        val METADATA_METHODS = setOf(
            "as",
            "alias",
        )

        val ELEMENT_INTERACTION_METHODS = setOf(
            "append",
            "clear",
            "click",
            "context click",
            "double click",
            "download",
            "drag and drop to",
            "hover",
            "paste",
            "press",
            "press enter",
            "press escape",
            "press tab",
            "scroll into view",
            "scroll to",
            "select option",
            "select option containing text",
            "select option by value",
            "select radio",
            "send keys",
            "set selected",
            "set value",
            "submit",
            "type",
            "unfocus",
            "upload file",
            "upload from classpath",
        )
    }
}
