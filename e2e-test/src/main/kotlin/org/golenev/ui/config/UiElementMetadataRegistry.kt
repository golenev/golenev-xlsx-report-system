package org.golenev.ui.config

object UiElementMetadataRegistry {

    private val locatorsByAlias = ThreadLocal.withInitial {
        linkedMapOf<String, MutableSet<String>>()
    }

    fun register(
        alias: String,
        locator: String,
    ) {
        val normalizedAlias = alias.trim()
        val normalizedLocator = locator.trim()

        if (normalizedAlias.isBlank() || normalizedLocator.isBlank()) {
            return
        }

        locatorsByAlias.get()
            .getOrPut(normalizedAlias) { linkedSetOf() }
            .add(normalizedLocator)
    }

    fun resolveLocator(alias: String): String? {
        return locatorsByAlias.get()[alias.trim()]
            ?.singleOrNull()
    }

    fun hasConflict(alias: String): Boolean {
        return locatorsByAlias.get()[alias.trim()]
            ?.let { it.size > 1 }
            ?: false
    }

    fun clear() {
        locatorsByAlias.remove()
    }
}
