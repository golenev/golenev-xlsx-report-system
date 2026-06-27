package org.golenev.ui.pages

import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Selenide.`$`

/**
 * Component Object warning popup, который отображает пользователю блокирующие предупреждения.
 */
class WarningPopup {

    /** Закрывает warning popup и проверяет, что карточка попапа исчезла. */
    fun close() {
        `$`(".popup-actions .secondary-btn").click()
        `$`(".popup-card").shouldBe(disappear.because("попап должен закрыться после нажатия кнопки закрытия"))
    }
}
