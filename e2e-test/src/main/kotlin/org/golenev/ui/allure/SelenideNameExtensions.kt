package org.golenev.ui.allure

import com.codeborne.selenide.ElementsCollection
import com.codeborne.selenide.SelenideElement

fun SelenideElement.name(alias: String): SelenideElement {
    UiElementNameRegistry.register(getSearchCriteria(), alias)
    return this
}

fun ElementsCollection.name(alias: String): ElementsCollection {
    UiElementNameRegistry.register(toString(), alias)
    return this
}
