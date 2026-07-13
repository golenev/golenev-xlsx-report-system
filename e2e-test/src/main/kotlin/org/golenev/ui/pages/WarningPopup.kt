package org.golenev.ui.pages

import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.exactText
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.SelenideElement
import org.golenev.ui.allure.name
import io.qameta.allure.Step

/**
 * Component Object warning popup, который отображает пользователю блокирующие предупреждения.
 */
class WarningPopup {

    private val message: SelenideElement = `$`(".popup-message").name("Текст warning popup.")

    private val title: SelenideElement = `$`(".popup-title").name("Заголовок warning popup.")

    private val closeButton: SelenideElement = `$`(".popup-actions .secondary-btn").name("Кнопка закрытия warning popup.")

    private val card: SelenideElement = `$`(".popup-card").name("Карточка warning popup.")

    /** Проверяет стандартный warning popup о незаполненных статусах перед остановкой регресса. */
    @Step("Проверяем стандартный warning popup о незаполненных статусах перед остановкой регресса")
    fun checkDefaultRegressionWarning() {
        message.shouldHave(exactText("Перед остановкой регресса заполните результаты для всех тест-кейсов.").because("попап должен объяснять, почему нельзя остановить регресс без заполненных статусов"))
        title.shouldHave(exactText("Не все статусы заполнены").because("заголовок попапа должен указывать на незаполненные статусы"))
    }

    /** Закрывает warning popup и проверяет, что карточка попапа исчезла. */
    @Step("Закрываем warning popup и проверяем, что карточка попапа исчезла")
    fun close() {
        closeButton.click()
        card.shouldBe(disappear.because("попап должен закрыться после нажатия кнопки закрытия"))
    }
}
