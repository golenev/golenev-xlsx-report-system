package org.golenev.ui.pages

import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.exactText
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.SelenideElement

/**
 * Component Object warning popup, который отображает пользователю блокирующие предупреждения.
 */
class WarningPopup {

    private val message: SelenideElement get() = `$`(MESSAGE_LOCATOR).asReportElement("Текст warning popup.", MESSAGE_LOCATOR)

    private val title: SelenideElement get() = `$`(TITLE_LOCATOR).asReportElement("Заголовок warning popup.", TITLE_LOCATOR)

    private val closeButton: SelenideElement get() = `$`(CLOSE_BUTTON_LOCATOR).asReportElement("Кнопка закрытия warning popup.", CLOSE_BUTTON_LOCATOR)

    private val card: SelenideElement get() = `$`(CARD_LOCATOR).asReportElement("Карточка warning popup.", CARD_LOCATOR)

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
    private companion object {
        private const val MESSAGE_LOCATOR = ".popup-message"
        private const val TITLE_LOCATOR = ".popup-title"
        private const val CLOSE_BUTTON_LOCATOR = ".popup-actions .secondary-btn"
        private const val CARD_LOCATOR = ".popup-card"
    }
}
