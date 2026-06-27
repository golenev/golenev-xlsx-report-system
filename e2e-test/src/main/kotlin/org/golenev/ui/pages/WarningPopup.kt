package org.golenev.ui.pages

import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.exactText
import com.codeborne.selenide.Selenide.`$`

/**
 * Component Object warning popup, который отображает пользователю блокирующие предупреждения.
 */
class WarningPopup {

    /** Проверяет стандартный warning popup о незаполненных статусах перед остановкой регресса. */
    fun checkDefaultRegressionWarning() {
        `$`(".popup-message").shouldHave(exactText("Перед остановкой регресса заполните результаты для всех тест-кейсов.").because("попап должен объяснять, почему нельзя остановить регресс без заполненных статусов"))
        `$`(".popup-title").shouldHave(exactText("Не все статусы заполнены").because("заголовок попапа должен указывать на незаполненные статусы"))
    }

    /** Закрывает warning popup и проверяет, что карточка попапа исчезла. */
    fun close() {
        `$`(".popup-actions .secondary-btn").click()
        `$`(".popup-card").shouldBe(disappear.because("попап должен закрыться после нажатия кнопки закрытия"))
    }
}
