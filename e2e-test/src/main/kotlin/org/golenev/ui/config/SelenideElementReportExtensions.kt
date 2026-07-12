package org.golenev.ui.config

import com.codeborne.selenide.SelenideElement

fun SelenideElement.reportAs(alias: String): SelenideElement {
    UiElementMetadataRegistry.register(
        alias = alias,
        locator = getSearchCriteria(),
    )

    return `as`(alias)
}
