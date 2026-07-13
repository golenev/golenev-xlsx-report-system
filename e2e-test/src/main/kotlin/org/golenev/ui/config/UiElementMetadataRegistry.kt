package org.golenev.ui.config

object UiElementMetadataRegistry {

    private val locatorByAlias = ThreadLocal.withInitial {
        linkedMapOf<String, String>()
    }

    fun register(
        alias: String,
        locator: String,
    ) {
        val normalizedAlias = alias.trim()
        val normalizedLocator = locator.trim()

        if (
            normalizedAlias.isBlank() ||
            normalizedLocator.isBlank()
        ) {
            return
        }

        locatorByAlias.get()[normalizedAlias] = normalizedLocator
    }

    fun resolveLocator(alias: String): String? {
        return locatorByAlias.get()[alias.trim()]
    }

    fun clear() {
        locatorByAlias.remove()
    }
}
