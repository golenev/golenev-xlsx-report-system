package org.golenev.ui.pages

import com.codeborne.selenide.SelenideElement

/** Добавляет к алиасу элемента технический локатор, чтобы listener мог вывести его в Allure-attachment. */
internal fun SelenideElement.asReportElement(alias: String, locator: String): SelenideElement =
    `as`("$alias$REPORT_LOCATOR_SEPARATOR$locator")

internal const val REPORT_LOCATOR_SEPARATOR = " || locator: "
