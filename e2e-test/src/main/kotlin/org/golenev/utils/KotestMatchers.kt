package org.golenev.utils

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

fun <T> T.shouldBe(
    expected: T,
    message: String,
) {
    withClue(message) {
        this shouldBe expected
    }
}
