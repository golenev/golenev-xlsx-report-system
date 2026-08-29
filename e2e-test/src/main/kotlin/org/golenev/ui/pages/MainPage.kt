package org.golenev.ui.pages

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import org.golenev.ui.allure.name
import io.qameta.allure.Step

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

    private val headerTitle: SelenideElement =
        element("h1").name("Заголовок страницы, по которому проверяется успешное открытие или обновление Test Report.")

    @Step("Переходим по базовому URL и дожидаемся отображения заголовка Test Report")
    fun open() {
        Selenide.open("/")
        checkTitle()
    }

    @Step("Обновляем страницу браузера и дожидаемся отображения заголовка Test Report")
    fun refreshCurrentPage() {
        Selenide.refresh()
        checkTitle()
    }

    @Step("Проверяем текст заголовка страницы Test Report")
    fun checkTitle() {
        headerTitle.shouldHave(text("Test Report").because("после открытия страницы должен отображаться заголовок отчета"))
    }

}
