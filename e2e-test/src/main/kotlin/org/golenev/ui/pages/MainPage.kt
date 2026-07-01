package org.golenev.ui.pages

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement

/**
 * Page Object главной страницы Test Report, который хранит действия уровня страницы и входные точки к вложенным компонентам.
 */
class MainPage {

    /** Таблица тест-кейсов на главной странице. */
    val testCaseTable: TestCaseTable by lazy { TestCaseTable() }

    /** Глобальный виджет управления regression run в шапке страницы. */
    val regressionWidget: RegressionWidget by lazy { RegressionWidget() }

    /** Warning popup, который появляется при невозможности выполнить действие. */
    val warningPopup: WarningPopup by lazy { WarningPopup() }

    /** Заголовок страницы, по которому проверяется успешное открытие или обновление Test Report. */
    private val headerTitle: SelenideElement = element("h1")

    /** Элемент body страницы, по которому можно снять фокус с активного поля. */
    private val body: SelenideElement get() = `$`("body")

    /** Открывает главную страницу Test Report и проверяет, что заголовок отображается. */
    fun open() {
        Selenide.open("/")
        checkTitle()
    }

    /** Обновляет текущую страницу браузера и повторно проверяет заголовок Test Report. */
    fun refreshCurrentPage() {
        Selenide.refresh()
        checkTitle()
    }

    /** Проверяет, что на странице отображается ожидаемый заголовок Test Report. */
    fun checkTitle() {
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

    /** Снимает фокус с активного поля кликом по body страницы. */
    fun unFocus() {
        body.click()
    }

}
