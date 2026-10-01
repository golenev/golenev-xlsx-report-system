package org.golenev.ui.pages

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner.url
import io.qameta.allure.Step

/**
 * Page Object главной страницы Test Report, который хранит действия уровня страницы и входные точки к вложенным компонентам.
 */
class MainPage {

    val testCaseEditor: TestCaseEditorModal by lazy { TestCaseEditorModal() }
    val header: AppHeader by lazy { AppHeader(testCaseEditor) }
    val columnFilterPanel: ColumnFilterPanel by lazy { ColumnFilterPanel { testCaseTable.checkColumnFilterActive(it) } }

    /** Таблица тест-кейсов на главной странице. */
    val testCaseTable: TestCaseTable by lazy { TestCaseTable(testCaseEditor, columnFilterPanel) }
    val tableViewToolbar: TableViewToolbar by lazy { TableViewToolbar(testCaseTable) }

    /** Warning popup, который появляется при невозможности выполнить действие. */
    val warningPopup: WarningPopup by lazy { WarningPopup() }

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
        header.checkTitle()
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
