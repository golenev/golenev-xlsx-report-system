package org.golenev.ui.pages

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner.url
import io.qameta.allure.Step

/**
 * Page Object главной страницы Test Report, который хранит действия уровня страницы и входные точки к вложенным компонентам.
 *
 * Собирает компоненты обычными полями и передаёт зависимости через их конструкторы.
 * Полный компонент передаётся для нескольких связанных проверок, функция — для отдельной проверки
 * результата действия. Это два используемых способа связи, а не различие в жизненном цикле компонентов.
 * Функции не выполняются при передаче: проверка применения фильтра обращается к таблице только
 * после действия пользователя, когда все поля страницы уже инициализированы.
 * Бизнес-последовательность остаётся в тесте, ожидания готовности и результата — в компонентах.
 */
class MainPage {

    /** Общий редактор создания и изменения тест-кейса; владеет футером, сценарием и диалогом несохранённых изменений. */
    val testCaseEditor: TestCaseEditorModal = TestCaseEditorModal()

    /** Шапка с заголовком и кнопкой Add Row; готовность открытого редактора проверяет через [testCaseEditor]. */
    val header: AppHeader = AppHeader(testCaseEditor)

    /**
     * Панель черновика фильтра выбранной колонки.
     * После применения вызывает проверку активной кнопки в [testCaseTable]; callback не выполняется при инициализации поля.
     */
    val columnFilterPanel: ColumnFilterPanel = ColumnFilterPanel { testCaseTable.checkColumnFilterActive(it) }

    /** Таблица тест-кейсов на главной странице. */
    val testCaseTable: TestCaseTable = TestCaseTable(testCaseEditor, columnFilterPanel)

    /** Управление группировкой и активными фильтрами; проверки результата в строках и заголовках делегирует [testCaseTable]. */
    val tableViewToolbar: TableViewToolbar = TableViewToolbar(testCaseTable)

    /** Warning popup, который появляется при невозможности выполнить действие. */
    val warningPopup: WarningPopup = WarningPopup()

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
