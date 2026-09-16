package org.golenev.ui.pages

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import com.codeborne.selenide.WebDriverRunner.url
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

    @Step("Переходим на главную страницу с параметрами представления таблицы: {query}")
    fun openWithQuery(query: String) {
        Selenide.open("/?${query.removePrefix("?")}")
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

    @Step("Проверяем параметры URL представления таблицы: {expectedParameters}")
    fun checkUrlParameters(expectedParameters: List<String>) {
        val currentUrl = url()
        expectedParameters.forEach { parameter ->
            check(currentUrl.contains(parameter)) {
                "URL '$currentUrl' должен содержать параметр '$parameter'"
            }
        }
    }

    @Step("Проверяем отсутствие параметров в URL представления таблицы: {unexpectedParameters}")
    fun checkUrlDoesNotContain(unexpectedParameters: List<String>) {
        val currentUrl = url()
        unexpectedParameters.forEach { parameter ->
            check(!currentUrl.contains(parameter)) {
                "URL '$currentUrl' не должен содержать параметр '$parameter'"
            }
        }
    }

}
