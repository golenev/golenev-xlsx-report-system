package org.golenev.tests.ui.filters

import io.qameta.allure.AllureId
import org.golenev.ui.pages.mainPage
import org.golenev.utils.step
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
@DisplayName("UI: Состояние фильтров и группировки таблицы в URL")
class TableViewUrlStateUiTest : FilterUiTestBase() {

    @Test
    @AllureId("324")
    @DisplayName("Применённые фильтры и группировка восстанавливаются после обновления страницы")
    fun appliedViewStateIsRestoredAfterRefresh() {
        val fixture = filterFixture()
        step("Добавляем три тест-кейса: два в категории ${fixture.categoryAlpha} со статусами «Готово» и «Бэклог», один в категории ${fixture.categoryBeta} со статусом «Готово»; приоритеты «Critical», «Blocker» и «Medium»") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем главную страницу") {
            mainPage.open()
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Проверяем загрузку подготовленной строки таблицы") { mainPage.testCaseTable.checkRowVisible(fixture.firstId) }
        step("Открываем фильтр колонки") {
            mainPage.testCaseTable.openColumnFilter("category")
        }
        step("Проверяем открытие фильтра колонки") { mainPage.columnFilterPanel.checkOpened("category") }
        listOf(fixture.categoryAlpha).forEach { filterValue ->
            step("Выбираем значение фильтра $filterValue") { mainPage.columnFilterPanel.selectFilterValue("category", filterValue) }
        }
        step("Применяем фильтр колонки") {
            mainPage.columnFilterPanel.applyColumnFilter("category")
        }
        step("Проверяем закрытие панели фильтра") { mainPage.columnFilterPanel.checkClosed("category") }
        step("Проверяем применение фильтра колонки") { mainPage.testCaseTable.checkColumnFilterActive("category") }
        step("Открываем фильтр колонки") {
            mainPage.testCaseTable.openColumnFilter("priority")
        }
        step("Проверяем открытие фильтра колонки") { mainPage.columnFilterPanel.checkOpened("priority") }
        listOf("Critical", "Blocker").forEach { filterValue ->
            step("Выбираем значение фильтра $filterValue") { mainPage.columnFilterPanel.selectFilterValue("priority", filterValue) }
        }
        step("Применяем фильтр колонки") {
            mainPage.columnFilterPanel.applyColumnFilter("priority")
        }
        step("Проверяем закрытие панели фильтра") { mainPage.columnFilterPanel.checkClosed("priority") }
        step("Проверяем применение фильтра колонки") { mainPage.testCaseTable.checkColumnFilterActive("priority") }
        step("Открываем таблицу, применяем фильтры и группировку") {
            mainPage.tableViewToolbar.groupBy("category")
        }
        step("Проверяем, что ссылка на таблицу сохраняет выбранные фильтры и группировку") {
            mainPage.checkUrlParameters(
                listOf(
                    "filter.category=${encodeQueryValue(fixture.categoryAlpha)}",
                    "filter.priority=Critical",
                    "filter.priority=Blocker",
                    "groupBy=category",
                ),
            )
        }
        step("Обновляем главную страницу") {
            mainPage.refreshCurrentPage()
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Обновляем страницу и проверяем восстановление выбранных фильтров и группировки") {
            mainPage.tableViewToolbar.checkActiveFiltersCount(2)
            mainPage.tableViewToolbar.checkGrouping("category")
        }
        step("Проверяем наличие заголовков групп") { mainPage.testCaseTable.checkGroupsPresent() }
        step("Обновляем страницу и проверяем восстановление выбранных фильтров и группировки") {
            mainPage.testCaseTable.checkGroupVisible("category", fixture.categoryAlpha)
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.thirdId)
        }
    }

    @Test
    @AllureId("325")
    @DisplayName("Прямая ссылка поддерживает кириллицу и несколько значений одного фильтра")
    fun directUrlSupportsCyrillicAndRepeatedValues() {
        val fixture = filterFixture()
        step("Добавляем три тест-кейса: два в категории ${fixture.categoryAlpha} со статусами «Готово» и «Бэклог», один в категории ${fixture.categoryBeta} со статусом «Готово»; приоритеты «Critical», «Blocker» и «Medium»") {
            createPreliminaryTestDataViaApi(fixture)
        }
        val query = listOf(
            "filter.category=${encodeQueryValue(fixture.categoryAlpha)}",
            "filter.priority=Critical",
            "filter.priority=Blocker",
            "groupBy=category",
        ).joinToString("&")
        step("Открываем прямую ссылку с кириллическим и многозначным состоянием фильтров") {
            mainPage.openWithQuery(query)
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Проверяем восстановленное по ссылке представление предварительных тест-кейсов") {
            mainPage.tableViewToolbar.checkActiveFiltersCount(2)
            mainPage.tableViewToolbar.checkActiveFilter("category", fixture.categoryAlpha)
            mainPage.tableViewToolbar.checkGrouping("category")
        }
        step("Проверяем наличие заголовков групп") { mainPage.testCaseTable.checkGroupsPresent() }
        step("Проверяем восстановленное по ссылке представление предварительных тест-кейсов") {
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.thirdId)
        }
    }

    @Test
    @AllureId("326")
    @DisplayName("Неизвестные параметры URL игнорируются без нарушения работы таблицы")
    fun invalidUrlParametersAreIgnored() {
        val fixture = filterFixture()
        step("Добавляем три тест-кейса: два в категории ${fixture.categoryAlpha} со статусами «Готово» и «Бэклог», один в категории ${fixture.categoryBeta} со статусом «Готово»; приоритеты «Critical», «Blocker» и «Medium»") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем прямую ссылку с неизвестными фильтром, условием и группировкой") {
            mainPage.openWithQuery("filter.unknown=value&filter.testId.mode=wrong&groupBy=unknown")
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Проверяем исходное представление и доступность предварительных тест-кейсов") {
            mainPage.tableViewToolbar.checkActiveFiltersCount(0)
            mainPage.tableViewToolbar.checkGroupingInactive()
        }
        step("Проверяем отсутствие заголовков групп") { mainPage.testCaseTable.checkGroupsAbsent() }
        step("Проверяем исходное представление и доступность предварительных тест-кейсов") {
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowVisible(fixture.thirdId)
        }
    }
}
