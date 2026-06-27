package org.golenev.utils

import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.SelenideElement
import com.codeborne.selenide.TypeOptions
import java.time.Duration

fun SelenideElement.typeOf(text: String, speed: Long = 10): SelenideElement {
    val options = TypeOptions
        .text(text)
        .withDelay(Duration.ofMillis(speed))
    return this.type(options)
}

/**
 * Проверяет видимость поля ввода с человекочитаемым названием для диагностического сообщения.
 */
internal fun SelenideElement.shouldBeVisibleForInput(fieldName: String): SelenideElement =
    shouldBe(visible.because("поле $fieldName должно быть видимым для ввода значения"))

const val CENTER: String = "{block: \"center\", behavior: \"auto\"}"