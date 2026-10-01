package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.element
import io.qameta.allure.Step
import org.golenev.ui.allure.name

/**
 * Component Object шапки страницы: проверяет заголовок и открывает создание тест-кейса.
 *
 * Конструктор получает редактор, чтобы после клика по Add Row делегировать ему ожидание
 * готовности режима создания. Шапка владеет кнопкой, редактор — локаторами и проверками модалки.
 * Передача зависимости не открывает редактор; открытие явно вызывается в бизнес-шаге теста.
 *
 * @param testCaseEditor общий редактор страницы, проверяющий результат открытия из шапки.
 */
class AppHeader(private val testCaseEditor: TestCaseEditorModal) {
    private val addRowButton =
        `$`("[data-action='add-row']")
            .name("Кнопка Add Row, которая открывает модальный редактор нового тест-кейса.")

    private val headerTitle = element("h1").name("Заголовок главной страницы Test Report")

    @Step("Проверяем текст заголовка страницы Test Report")
    fun checkTitle() {
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

    @Step("Нажимаем Add Row и проверяем появление модального редактора создания")
    fun openCreateEditor() {
        addRowButton.shouldBe(visible).shouldBe(enabled.because("кнопка создания должна быть доступна")).click()
        testCaseEditor.checkCreateModeReady()
    }
}
