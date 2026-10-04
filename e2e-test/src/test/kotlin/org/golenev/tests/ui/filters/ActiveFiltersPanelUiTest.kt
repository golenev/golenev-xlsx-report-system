package org.golenev.tests.ui.filters

import io.qameta.allure.AllureId
import org.golenev.ui.pages.mainPage
import org.golenev.utils.step
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
@DisplayName("UI: Панель активных фильтров таблицы тест-кейсов")
class ActiveFiltersPanelUiTest : FilterUiTestBase() {

    @Test
    @AllureId("318")
    @DisplayName("Применённые фильтры отображаются отдельными человекочитаемыми чипами")
    fun appliedFiltersAreShownAsChips() {
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
            mainPage.testCaseTable.openColumnFilter("generalStatus")
        }
        step("Проверяем открытие фильтра колонки") { mainPage.columnFilterPanel.checkOpened("generalStatus") }
        listOf("Готово").forEach { filterValue ->
            step("Выбираем значение фильтра $filterValue") { mainPage.columnFilterPanel.selectFilterValue("generalStatus", filterValue) }
        }
        step("Применяем фильтр колонки") {
            mainPage.columnFilterPanel.applyColumnFilter("generalStatus")
        }
        step("Проверяем закрытие панели фильтра") { mainPage.columnFilterPanel.checkClosed("generalStatus") }
        step("Проверяем применение фильтра колонки") { mainPage.testCaseTable.checkColumnFilterActive("generalStatus") }
        step("Проверяем два обозначения с понятными описаниями активных фильтров") {
            mainPage.tableViewToolbar.checkActiveFiltersCount(2)
            mainPage.tableViewToolbar.checkActiveFilter("category", fixture.categoryAlpha)
            mainPage.tableViewToolbar.checkActiveFilter("generalStatus", "Готово")
        }
    }

    @Test
    @AllureId("319")
    @DisplayName("Удаление одного чипа сохраняет остальные активные фильтры")
    fun oneFilterCanBeRemovedWithoutResettingOthers() {
        val fixture = filterFixture()
        step("Добавляем три тест-кейса: два в категории ${fixture.categoryAlpha} со статусами «Готово» и «Бэклог», один в категории ${fixture.categoryBeta} со статусом «Готово»; приоритеты «Critical», «Blocker» и «Medium»") {
            createPreliminaryTestDataViaApi(fixture)
        }
        with(mainPage) {
            step("Открываем главную страницу") { open() }
            step("Проверяем заголовок открытой страницы") { header.checkTitle() }
            step("Проверяем загрузку подготовленной строки таблицы") { testCaseTable.checkRowVisible(fixture.firstId) }
            step("Открываем фильтр колонки") {
                testCaseTable.openColumnFilter("category")
            }
            step("Проверяем открытие фильтра колонки") { columnFilterPanel.checkOpened("category") }
            listOf(fixture.categoryAlpha).forEach { filterValue ->
                step("Выбираем значение фильтра $filterValue") { columnFilterPanel.selectFilterValue("category", filterValue) }
            }
            step("Применяем фильтр колонки") {
                columnFilterPanel.applyColumnFilter("category")
            }
            step("Проверяем закрытие панели фильтра") { columnFilterPanel.checkClosed("category") }
            step("Проверяем применение фильтра колонки") { testCaseTable.checkColumnFilterActive("category") }
            step("Открываем фильтр колонки") {
                testCaseTable.openColumnFilter("generalStatus")
            }
            step("Проверяем открытие фильтра колонки") { columnFilterPanel.checkOpened("generalStatus") }
            listOf("Готово").forEach { filterValue ->
                step("Выбираем значение фильтра $filterValue") { columnFilterPanel.selectFilterValue("generalStatus", filterValue) }
            }
            step("Применяем фильтр колонки") {
                columnFilterPanel.applyColumnFilter("generalStatus")
            }
            step("Проверяем закрытие панели фильтра") { columnFilterPanel.checkClosed("generalStatus") }
            step("Проверяем применение фильтра колонки") { testCaseTable.checkColumnFilterActive("generalStatus") }
        }
        step("Убираем только фильтр статуса") {
            mainPage.tableViewToolbar.removeActiveFilter("generalStatus")
        }
        step("Проверяем сброс фильтра колонки") { mainPage.testCaseTable.checkColumnFilterInactive("generalStatus") }
        step("Проверяем сохранение фильтра категории и возврат второго тест-кейса этой категории") {
            with(mainPage) {
                tableViewToolbar.checkActiveFiltersCount(1)
                tableViewToolbar.checkActiveFilter("category", fixture.categoryAlpha)
                testCaseTable.checkRowVisible(fixture.firstId)
                testCaseTable.checkRowVisible(fixture.secondId)
                testCaseTable.checkRowDisappeared(fixture.thirdId)
            }
        }
    }

    @Test
    @AllureId("320")
    @DisplayName("Общий сброс удаляет все фильтры и возвращает полный набор строк")
    fun allFiltersCanBeClearedTogether() {
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
        listOf("Critical").forEach { filterValue ->
            step("Выбираем значение фильтра $filterValue") { mainPage.columnFilterPanel.selectFilterValue("priority", filterValue) }
        }
        step("Применяем фильтр колонки") {
            mainPage.columnFilterPanel.applyColumnFilter("priority")
        }
        step("Проверяем закрытие панели фильтра") { mainPage.columnFilterPanel.checkClosed("priority") }
        step("Проверяем применение фильтра колонки") { mainPage.testCaseTable.checkColumnFilterActive("priority") }
        step("Сбрасываем все фильтры одной командой") {
            mainPage.tableViewToolbar.clearAllFilters()
        }
        step("Проверяем отсутствие активных фильтров") { mainPage.tableViewToolbar.checkActiveFiltersCount(0) }
        step("Проверяем отсутствие фильтров и возврат всех предварительно созданных строк") {
            mainPage.tableViewToolbar.checkActiveFiltersCount(0)
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowVisible(fixture.thirdId)
            mainPage.checkUrlDoesNotContain(listOf("filter."))
        }
    }
}
