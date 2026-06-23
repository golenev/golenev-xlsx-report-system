package org.golenev.ui.pages

import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.exactText
import com.codeborne.selenide.Selenide.`$`

class WarningPopup {
    fun shouldHaveDefaultRegressionWarning() {
        shouldHaveMessage("Перед остановкой регресса заполните результаты для всех тест-кейсов.")
        shouldHaveTitle("Не все статусы заполнены")
    }

    fun shouldHaveTitle(expectedTitle: String) {
        `$`(".popup-title").shouldHave(exactText(expectedTitle).because("заголовок попапа должен указывать на незаполненные статусы"))
    }

    fun shouldHaveMessage(expectedMessage: String) {
        `$`(".popup-message").shouldHave(exactText(expectedMessage).because("попап должен объяснять, почему нельзя остановить регресс без заполненных статусов"))
    }

    fun close() {
        `$`(".popup-actions .secondary-btn").click()
        `$`(".popup-card").shouldBe(disappear.because("попап должен закрыться после нажатия кнопки закрытия"))
    }
}
