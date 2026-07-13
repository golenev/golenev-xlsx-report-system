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
import java.util.ArrayDeque
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
            event.isConditionEvent() -> {
                currentState.conditionEvents.addLast(
                    EventContext(
                        parentStepId = lifecycle.currentTestCaseOrStep.orElse(null),
                        startedAt = System.currentTimeMillis(),
                    )
                )
            }

            event.isElementInteraction() -> {
                val explicitCondition = currentState.pendingCondition
                    ?.takeIf { condition ->
                        condition.matches(event)
                    }

                currentState.pendingCondition = null

                currentState.interactionEvents.addLast(
                    InteractionContext(
                        parentStepId = lifecycle.currentTestCaseOrStep.orElse(null),
                        startedAt = System.currentTimeMillis(),
                        details = event.toInteractionDetails(
                            explicitCondition = explicitCondition,
                        ),
                    )
                )
            }

            !event.isMetadataEvent() -> {
                currentState.pendingCondition = null
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
            currentState.pendingCondition == null &&
            currentState.conditionEvents.isEmpty() &&
            currentState.interactionEvents.isEmpty()
        ) {
            state.remove()
        }
    }

    private fun handleConditionEvent(
        event: LogEvent,
        currentState: ListenerState,
    ) {
        val eventContext = currentState.conditionEvents.pollLast()
            ?: EventContext(
                parentStepId = lifecycle.currentTestCaseOrStep.orElse(null),
                startedAt = System.currentTimeMillis(),
            )

        val conditionDetails = event.toConditionDetails()

        when (event.status) {
            PASS -> {
                currentState.pendingCondition = conditionDetails
            }

            FAIL -> {
                currentState.pendingCondition = null

                publishStep(
                    event = event,
                    parentStepId = eventContext.parentStepId,
                    startedAt = eventContext.startedAt,
                    interactionDetails = InteractionDetails(
                        alias = conditionDetails.alias,
                        locator = conditionDetails.locator,
                        explicitCondition = conditionDetails.condition,
                        implicitCondition = null,
                        because = conditionDetails.because,
                    ),
                )
            }

            else -> {
                currentState.pendingCondition = null
            }
        }
    }

    private fun handleInteractionEvent(
        event: LogEvent,
        currentState: ListenerState,
    ) {
        val interactionContext = currentState.interactionEvents.pollLast()
            ?: return

        publishStep(
            event = event,
            parentStepId = interactionContext.parentStepId,
            startedAt = interactionContext.startedAt,
            interactionDetails = interactionContext.details,
        )
    }

    private fun publishStep(
        event: LogEvent,
        parentStepId: String?,
        startedAt: Long,
        interactionDetails: InteractionDetails,
    ) {
        val stepId = UUID.randomUUID().toString()

        val stepResult = StepResult()
            .setName(
                interactionDetails.alias
                    ?: interactionDetails.locator
                    ?: event.element
            )
            .setStart(startedAt)

        if (parentStepId == null) {
            lifecycle.startStep(
                stepId,
                stepResult,
            )
        } else {
            lifecycle.startStep(
                parentStepId,
                stepId,
                stepResult,
            )
        }

        try {
            attachReadableSelenideInfo(
                interactionDetails = interactionDetails,
            )

            if (event.status == FAIL) {
                attachScreenshot()
                attachPageSource()
            }

            lifecycle.updateStep(stepId) { step ->
                step.status = event.toAllureStatus()
                step.statusDetails = event.error?.toStatusDetails()
            }
        } finally {
            lifecycle.stopStep(stepId)
        }
    }

    private fun ConditionDetails.matches(event: LogEvent): Boolean {
        val eventLocator = event.element.resolveLocator()

        if (locator != null && eventLocator != null) {
            return locator == eventLocator
        }

        val eventAlias = event.element
            .takeUnless { it.looksLikeLocator() }
            ?.trim()

        return alias != null && alias == eventAlias
    }

    private fun LogEvent.toInteractionDetails(
        explicitCondition: ConditionDetails?,
    ): InteractionDetails {
        val elementIsLocator = element.looksLikeLocator()

        return InteractionDetails(
            alias = if (elementIsLocator) {
                explicitCondition?.alias
            } else {
                element.trim()
            },
            locator = element.resolveLocator()
                ?: explicitCondition?.locator,
            explicitCondition = explicitCondition?.condition,
            implicitCondition = implicitCondition(),
            because = explicitCondition?.because,
        )
    }

    private fun LogEvent.toConditionDetails(): ConditionDetails {
        val parsedCondition = subject.parseCondition()
        val elementIsLocator = element.looksLikeLocator()

        return ConditionDetails(
            alias = element
                .takeUnless { elementIsLocator }
                ?.trim(),
            locator = element.resolveLocator(),
            condition = parsedCondition.condition,
            because = parsedCondition.because,
        )
    }

    private fun attachReadableSelenideInfo(
        interactionDetails: InteractionDetails,
    ) {
        val conditions = buildList {
            interactionDetails.explicitCondition
                ?.takeIf { it.isNotBlank() }
                ?.let { condition ->
                    add("Явное: $condition")
                }

            interactionDetails.implicitCondition
                ?.takeIf { it.isNotBlank() }
                ?.let { condition ->
                    add("Неявное Selenide: $condition")
                }
        }.joinToString(System.lineSeparator())

        val attachmentText = buildString {
            appendLine("Алиас элемента:")
            appendLine(interactionDetails.alias.orEmpty())
            appendLine()

            appendLine("Конечный локатор:")
            appendLine(interactionDetails.locator.orEmpty())
            appendLine()

            appendLine("Условия для успешного взаимодействия:")
            appendLine(conditions)
            appendLine()

            appendLine("Ожидаемая причина:")
            appendLine(interactionDetails.because.orEmpty())
        }

        Allure.addAttachment(
            "Детали UI-взаимодействия",
            "text/plain",
            attachmentText,
        )
    }

    private fun LogEvent.implicitCondition(): String {
        val methodName = subject.methodName()

        return when (methodName) {
            "click",
            "double click",
            "context click",
            -> "clickable: interactable и enabled"

            "clear",
            "set value",
            "type",
            -> "editable: interactable, enabled и не readonly"

            "hover" ->
                "элемент должен существовать для выполнения hover"

            "send keys" ->
                "элемент должен существовать для отправки клавиш"

            "set selected" ->
                "элемент должен существовать для изменения selected-состояния"

            "submit" ->
                "элемент должен существовать для отправки формы"

            "unfocus" ->
                "элемент должен существовать для снятия фокуса"

            else ->
                ""
        }
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

        return subject.methodName() in ELEMENT_INTERACTION_METHODS
    }

    private fun LogEvent.isMetadataEvent(): Boolean {
        return subject.methodName() in METADATA_METHODS
    }

    private fun String.resolveLocator(): String? {
        val value = trim()

        return if (value.looksLikeLocator()) {
            value
        } else {
            UiElementMetadataRegistry.resolveLocator(value)
        }
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

        return value.startsWith("\$(") ||
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
        var pendingCondition: ConditionDetails? = null,
        val conditionEvents: ArrayDeque<EventContext> = ArrayDeque(),
        val interactionEvents: ArrayDeque<InteractionContext> = ArrayDeque(),
    )

    private data class EventContext(
        val parentStepId: String?,
        val startedAt: Long,
    )

    private data class InteractionContext(
        val parentStepId: String?,
        val startedAt: Long,
        val details: InteractionDetails,
    )

    private data class InteractionDetails(
        val alias: String?,
        val locator: String?,
        val explicitCondition: String?,
        val implicitCondition: String?,
        val because: String?,
    )

    private data class ConditionDetails(
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
            "clear",
            "click",
            "context click",
            "double click",
            "hover",
            "send keys",
            "set selected",
            "set value",
            "submit",
            "type",
            "unfocus",
        )
    }
}
