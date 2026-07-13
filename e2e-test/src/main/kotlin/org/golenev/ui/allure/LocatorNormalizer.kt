package org.golenev.ui.allure

object LocatorNormalizer {
    fun normalize(locator: String?): String {
        val trimmed = locator?.trim().orEmpty()
        if (trimmed.isBlank()) return ""

        return trimmed
            .removeSelenideWrapper()
            .removeCollectionWrapper()
            .removeByPrefix()
            .trim()
    }

    private fun String.removeSelenideWrapper(): String {
        val value = trim()
        if (!value.startsWith("$(") || !value.endsWith(")")) return value

        val content = value.substring(2, value.lastIndex).trim()
        if (content.length < 2) return content

        val quote = content.first()
        return if ((quote == '\'' || quote == '"') && content.last() == quote) {
            content.substring(1, content.lastIndex)
        } else {
            content
        }
    }

    private fun String.removeCollectionWrapper(): String {
        val value = trim()
        return when {
            value.startsWith("Elements collection") -> value.substringAfter("Elements collection").trim().trimStart(':').trim()
            value.startsWith("Collection") -> value.substringAfter("Collection").trim().trimStart(':').trim()
            else -> value
        }
    }

    private fun String.removeByPrefix(): String {
        val value = trim()
        return when {
            value.startsWith("By.cssSelector: ") -> value.removePrefix("By.cssSelector: ").trim()
            value.startsWith("By.xpath: ") -> value.removePrefix("By.xpath: ").trim()
            else -> value
        }
    }
}
