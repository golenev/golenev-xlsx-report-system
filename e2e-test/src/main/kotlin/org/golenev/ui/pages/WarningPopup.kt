package org.golenev.ui.pages

import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.exactText
import com.codeborne.selenide.Selenide.`$`

/**
 * Component Object warning popup, который отображает пользователю блокирующие предупреждения.
 */
class WarningPopup {
    /** Проверяет стандартный warning popup о незаполненных статусах перед остановкой регресса. */
    fun shouldHaveDefaultRegressionWarning() {
        shouldHaveMessage("Перед остановкой регресса заполните результаты для всех тест-кейсов.")
        shouldHaveTitle("Не все статусы заполнены")
    }

    /** Проверяет заголовок warning popup. */
    fun shouldHaveTitle(expectedTitle: String) {
        `$`(".popup-title").shouldHave(exactText(expectedTitle).because("заголовок попапа должен указывать на незаполненные статусы"))
    }

    /** Проверяет текст сообщения warning popup. */
    fun shouldHaveMessage(expectedMessage: String) {
        `$`(".popup-message").shouldHave(exactText(expectedMessage).because("попап должен объяснять, почему нельзя остановить регресс без заполненных статусов"))
    }

    /** Закрывает warning popup и проверяет, что карточка попапа исчезла. */
    fun close() {
        `$`(".popup-actions .secondary-btn").click()
        `$`(".popup-card").shouldBe(disappear.because("попап должен закрыться после нажатия кнопки закрытия"))
    }
}
