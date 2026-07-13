package org.golenev.ui.allure

object UiEventSubjectParser {
    private val checkOperations = listOf("should not have", "should not be", "should have", "should be", "should not", "should")
    private val actionConditions = linkedMapOf(
        "context click" to "clickable: interactable и enabled",
        "double click" to "clickable: interactable и enabled",
        "set selected" to "элемент должен существовать для изменения selected-состояния",
        "set value" to "editable: interactable, enabled и не readonly",
        "send keys" to "элемент должен существовать для отправки клавиш",
        "unfocus" to "элемент должен существовать для снятия фокуса",
        "submit" to "элемент должен существовать для отправки формы",
        "clear" to "editable: interactable, enabled и не readonly",
        "click" to "clickable: interactable и enabled",
        "hover" to "элемент должен существовать для выполнения hover",
        "type" to "editable: interactable, enabled и не readonly",
    )

    fun parse(subject: String): ParsedUiEvent? {
        val normalizedSubject = subject.trim()
        if (normalizedSubject.isBlank()) return null

        val check = parseCheck(normalizedSubject)
        if (check != null) return check

        val action = actionConditions.entries.firstOrNull { (operation) ->
            normalizedSubject == operation || normalizedSubject.startsWith("$operation(")
        } ?: return null

        return ParsedUiEvent(
            eventType = UiEventType.ACTION,
            operation = normalizedSubject,
            successCondition = action.value,
            because = null,
        )
    }

    private fun parseCheck(subject: String): ParsedUiEvent? {
        val operation = checkOperations.firstOrNull { subject == it || subject.startsWith("$it(") } ?: return null
        val condition = subject.removePrefix(operation).trim().removeOuterParentheses()
        val becauseResult = extractBecause(condition)
        return ParsedUiEvent(
            eventType = UiEventType.CHECK,
            operation = operation,
            successCondition = becauseResult.condition.ifBlank { null },
            because = becauseResult.because,
        )
    }

    private fun String.removeOuterParentheses(): String {
        val value = trim()
        if (!value.startsWith("(") || !value.endsWith(")")) return value
        var depth = 0
        value.forEachIndexed { index, char ->
            when (char) {
                '(' -> depth++
                ')' -> depth--
            }
            if (depth == 0 && index < value.lastIndex) return value
        }
        return value.substring(1, value.lastIndex).trim()
    }

    private fun extractBecause(condition: String): BecauseResult {
        val marker = "(because "
        var depth = 0
        var start = -1
        var index = 0
        while (index <= condition.length - marker.length) {
            val char = condition[index]
            if (char == '(') {
                if (condition.startsWith(marker, index)) {
                    start = index
                    break
                }
                depth++
            } else if (char == ')' && depth > 0) {
                depth--
            }
            index++
        }
        if (start < 0) return BecauseResult(condition.trim(), null)

        var end = -1
        depth = 0
        for (i in start until condition.length) {
            when (condition[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }
        }
        if (end < 0) return BecauseResult(condition.trim(), null)

        val because = condition.substring(start + marker.length, end).trim().ifBlank { null }
        val cleaned = (condition.substring(0, start) + condition.substring(end + 1)).trim()
        return BecauseResult(cleaned, because)
    }

    data class ParsedUiEvent(
        val eventType: UiEventType,
        val operation: String,
        val successCondition: String?,
        val because: String?,
    )

    private data class BecauseResult(val condition: String, val because: String?)
}
