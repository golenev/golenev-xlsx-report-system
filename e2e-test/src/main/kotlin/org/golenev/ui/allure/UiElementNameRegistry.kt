package org.golenev.ui.allure

object UiElementNameRegistry {
    private val aliasesByLocator = ThreadLocal.withInitial { linkedMapOf<String, String>() }

    fun register(locator: String, alias: String) {
        val normalizedLocator = LocatorNormalizer.normalize(locator)
        val normalizedAlias = alias.trim()
        if (normalizedLocator.isBlank() || normalizedAlias.isBlank()) return
        aliasesByLocator.get()[normalizedLocator] = normalizedAlias
    }

    fun findAlias(locator: String): String? = aliasesByLocator.get()[LocatorNormalizer.normalize(locator)]

    fun clear() {
        aliasesByLocator.remove()
    }
}
