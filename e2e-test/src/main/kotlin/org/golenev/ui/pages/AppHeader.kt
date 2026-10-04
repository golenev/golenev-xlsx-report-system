package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.element
import org.golenev.ui.allure.name

/**
 * Component Object шапки страницы: проверяет заголовок и открывает создание тест-кейса.
 *
 * Шапка владеет кнопкой Add Row. Проверку готовности модалки тест явно вызывает
 * отдельным шагом через TestCaseEditorModal; зависимость от редактора не требуется.
 */
class AppHeader {
    private val addRowButton =
        `$`("[data-action='add-row']")
            .name("Кнопка Add Row, которая открывает модальный редактор нового тест-кейса.")

    private val headerTitle = element("h1").name("Заголовок главной страницы Test Report")

    /**
     * Проверяем текст заголовка страницы Test Report
     */
    fun checkTitle() {
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

    /**
     * Нажимаем Add Row и проверяем появление модального редактора создания
     */
    fun openCreateEditor() {
        addRowButton.shouldBe(visible).shouldBe(enabled.because("кнопка создания должна быть доступна")).click()
    }
}
