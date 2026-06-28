package org.golenev.ui.pages

/**
 * Единая точка доступа к page/component objects UI-приложения.
 *
 * Экземпляры создаются лениво, чтобы тесты не объявляли `MainPage()`, `TestCaseTable()`
 * и остальные компоненты в каждом классе отдельно.
 */
object Application {
    /** Главная страница Test Report. */
    val mainPage: MainPage by lazy { MainPage() }

    /** Таблица тест-кейсов на главной странице. */
    val testCaseTable: TestCaseTable by lazy { TestCaseTable() }

    /** Глобальный виджет управления regression run в шапке страницы. */
    val regressionWidget: RegressionWidget by lazy { RegressionWidget() }

    /** Warning popup, который появляется при невозможности выполнить действие. */
    val warningPopup: WarningPopup by lazy { WarningPopup() }
}
