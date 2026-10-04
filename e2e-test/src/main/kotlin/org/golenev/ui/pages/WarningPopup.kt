package org.golenev.ui.pages

import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.exactText
import com.codeborne.selenide.Selenide.`$`
import org.golenev.ui.allure.name

/**
 * Component Object warning popup, который отображает пользователю блокирующие предупреждения.
 */
class WarningPopup {

    private val message = `$`(".popup-message").name("Текст warning popup.")

    private val title = `$`(".popup-title").name("Заголовок warning popup.")

    private val closeButton = `$`(".popup-actions .secondary-btn").name("Кнопка закрытия warning popup.")

    private val card = `$`(".popup-card").name("Карточка warning popup.")

    /**
     * Проверяем текст сообщения и заголовка warning popup о незаполненных статусах
     */
    fun checkDefaultRegressionWarning() {
        message.shouldHave(exactText("Перед остановкой регресса заполните результаты для всех тест-кейсов.").because("попап должен объяснять, почему нельзя остановить регресс без заполненных статусов"))
    }

    /**
     * Нажимаем кнопку закрытия warning popup и дожидаемся исчезновения карточки
     */
    fun close() {
        closeButton.click()
    }

    /**
     * Проверяем заголовок предупреждения о незаполненных статусах
     */
    fun checkDefaultRegressionWarningTitle() {
        title.shouldHave(exactText("Не все статусы заполнены").because("заголовок попапа должен указывать на незаполненные статусы"))
    }

    /**
     * Проверяем закрытие warning popup
     */
    fun checkClosed() {
        card.shouldBe(disappear.because("попап должен закрыться после нажатия кнопки закрытия"))
    }
}
