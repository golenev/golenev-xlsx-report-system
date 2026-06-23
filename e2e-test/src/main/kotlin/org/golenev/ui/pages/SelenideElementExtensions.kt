package org.golenev.ui.pages

import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.SelenideElement

/**
 * Проверяет видимость поля ввода с человекочитаемым названием для диагностического сообщения.
 */
internal fun SelenideElement.shouldBeVisibleForInput(fieldName: String): SelenideElement =
    shouldBe(visible.because("поле $fieldName должно быть видимым для ввода значения"))
