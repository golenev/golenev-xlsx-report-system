package org.golenev.ui.pages

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner.url

/**
 * Page Object главной страницы Test Report, который хранит действия уровня страницы и входные точки к вложенным компонентам.
 *
 * Собирает независимые компоненты обычными полями. Компоненты не получают соседние
 * Page Object или функции проверки результата. Открытие, действие и проверки разных элементов
 * тест вызывает явно в отдельных шагах; локаторы и Selenide-ожидания остаются в компонентах.
 */
class MainPage {

    /** Общий редактор создания и изменения тест-кейса; владеет футером, сценарием и диалогом несохранённых изменений. */
    val testCaseEditor: TestCaseEditorModal = TestCaseEditorModal()

    /** Шапка с заголовком и кнопкой Add Row; готовность редактора тест проверяет отдельно. */
    val header: AppHeader = AppHeader()

    /**
     * Панель черновика фильтра выбранной колонки.
     * После применения тест отдельно проверяет закрытие панели и активность кнопки в [testCaseTable].
     */
    val columnFilterPanel: ColumnFilterPanel = ColumnFilterPanel()

    /** Таблица тест-кейсов на главной странице. */
    val testCaseTable: TestCaseTable = TestCaseTable()

    /** Управление группировкой и активными фильтрами; проверки результата в [testCaseTable] явно вызывает тест. */
    val tableViewToolbar: TableViewToolbar = TableViewToolbar()

    /** Warning popup, который появляется при невозможности выполнить действие. */
    val warningPopup: WarningPopup = WarningPopup()

    /**
     * Переходим по базовому URL и дожидаемся отображения заголовка Test Report
     */
    fun open() {
        Selenide.open("/")
    }

    /**
     * Переходим на главную страницу с параметрами представления таблицы: {query}
     */
    fun openWithQuery(query: String) {
        Selenide.open("/?${query.removePrefix("?")}")
    }

    /**
     * Обновляем страницу браузера и дожидаемся отображения заголовка Test Report
     */
    fun refreshCurrentPage() {
        Selenide.refresh()
    }

    /**
     * Проверяем текст заголовка страницы Test Report
     */
    fun checkTitle() {
        header.checkTitle()
    }

    /**
     * Проверяем параметры URL представления таблицы: {expectedParameters}
     */
    fun checkUrlParameters(expectedParameters: List<String>) {
        val currentUrl = url()
        expectedParameters.forEach { parameter ->
            check(currentUrl.contains(parameter)) {
                "URL '$currentUrl' должен содержать параметр '$parameter'"
            }
        }
    }

    /**
     * Проверяем отсутствие параметров в URL представления таблицы: {unexpectedParameters}
     */
    fun checkUrlDoesNotContain(unexpectedParameters: List<String>) {
        val currentUrl = url()
        unexpectedParameters.forEach { parameter ->
            check(!currentUrl.contains(parameter)) {
                "URL '$currentUrl' не должен содержать параметр '$parameter'"
            }
        }
    }

}
