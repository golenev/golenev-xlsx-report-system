package com.example.e2e.ui.core

import com.codeborne.selenide.Condition

/**
 * Проверяет условие для контейнера через `shouldBe`.
 */
infix fun <T : IContainer> T.containerShould(condition: Condition): T = apply {
    self.shouldBe(condition)
}

/**
 * Проверяет отрицание условия для контейнера через `shouldNotBe`.
 */
infix fun <T : IContainer> T.containerShouldNot(condition: Condition): T = apply {
    self.shouldNotBe(condition)
}
