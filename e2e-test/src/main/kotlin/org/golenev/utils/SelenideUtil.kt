package org.golenev.utils

import com.codeborne.selenide.SelenideElement
import com.codeborne.selenide.TypeOptions
import java.time.Duration

fun SelenideElement.typeOf(text: String, speed: Long = 10): SelenideElement {
    val options = TypeOptions
        .text(text)
        .withDelay(Duration.ofMillis(speed))
    return this.type(options)
}

const val CENTER: String = "{block: \"center\", behavior: \"auto\"}"