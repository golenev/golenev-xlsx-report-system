package org.golenev.ui.pages

import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.exactText
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.SelenideElement

/**
 * Component Object warning popup, который отображает пользователю блокирующие предупреждения.
 */
class WarningPopup {

    /** Текст warning popup. */
    private val message: SelenideElement get() = `$`(".popup-message")

    /** Заголовок warning popup. */
    private val title: SelenideElement get() = `$`(".popup-title")

    /** Кнопка закрытия warning popup. */
    private val closeButton: SelenideElement get() = `$`(".popup-actions .secondary-btn")

    /** Карточка warning popup. */
    private val card: SelenideElement get() = `$`(".popup-card")

    /** Проверяет стандартный warning popup о незаполненных статусах перед остановкой регресса. */
    fun checkDefaultRegressionWarning() {
        message.shouldHave(exactText("Перед остановкой регресса заполните результаты для всех тест-кейсов.").because("попап должен объяснять, почему нельзя остановить регресс без заполненных статусов"))
        title.shouldHave(exactText("Не все статусы заполнены").because("заголовок попапа должен указывать на незаполненные статусы"))
    }

    /** Закрывает warning popup и проверяет, что карточка попапа исчезла. */
    fun close() {
        closeButton.click()
        card.shouldBe(disappear.because("попап должен закрыться после нажатия кнопки закрытия"))
    }
}
