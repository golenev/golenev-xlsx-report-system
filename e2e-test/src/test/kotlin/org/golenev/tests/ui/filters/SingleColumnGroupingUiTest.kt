package org.golenev.tests.ui.filters

import io.qameta.allure.AllureId
import org.golenev.ui.pages.mainPage
import org.golenev.utils.step
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
@DisplayName("UI: Группировка тест-кейсов по одной колонке")
class SingleColumnGroupingUiTest : FilterUiTestBase() {

    @Test
    @AllureId("321")
    @DisplayName("Строки группируются по Category, а выбранная группа сворачивается")
    fun rowsCanBeGroupedByCategoryAndCollapsed() {
        val fixture = filterFixture()
        step("Добавляем три тест-кейса: два в категории ${fixture.categoryAlpha} со статусами «Готово» и «Бэклог», один в категории ${fixture.categoryBeta} со статусом «Готово»; приоритеты «Critical», «Blocker» и «Medium»") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем главную страницу") {
            mainPage.open()
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Открываем таблицу и группируем тест-кейсы по категории") {
            mainPage.tableViewToolbar.groupBy("category")
        }
        step("Проверяем группы обеих категорий предварительных данных") {
            mainPage.testCaseTable.checkGroupVisible("category", fixture.categoryAlpha)
            mainPage.testCaseTable.checkGroupVisible("category", fixture.categoryBeta)
        }
        step("Сворачиваем первую категорию и проверяем скрытие только её тест-кейсов") {
            mainPage.testCaseTable.collapseGroup("category", fixture.categoryAlpha)
        }
        step("Проверяем свёрнутое состояние группы") { mainPage.testCaseTable.checkGroupCollapsed("category", fixture.categoryAlpha) }
        step("Сворачиваем первую категорию и проверяем скрытие только её тест-кейсов") {
            mainPage.testCaseTable.checkRowDisappeared(fixture.firstId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.secondId)
            mainPage.testCaseTable.checkRowVisible(fixture.thirdId)
        }
    }

    @Test
    @AllureId("322")
    @DisplayName("Выбор новой колонки группировки заменяет предыдущую группировку")
    fun newGroupingReplacesPreviousGrouping() {
        val fixture = filterFixture()
        step("Добавляем три тест-кейса: два в категории ${fixture.categoryAlpha} со статусами «Готово» и «Бэклог», один в категории ${fixture.categoryBeta} со статусом «Готово»; приоритеты «Critical», «Blocker» и «Medium»") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем главную страницу") {
            mainPage.open()
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Открываем таблицу и сначала группируем по категории") {
            mainPage.tableViewToolbar.groupBy("category")
            mainPage.testCaseTable.checkGroupVisible("category", fixture.categoryAlpha)
        }
        step("Переключаем единственную группировку на приоритет") {
            mainPage.tableViewToolbar.groupBy("priority")
        }
        step("Проверяем новую группировку и отсутствие заголовков прежней") {
            mainPage.tableViewToolbar.checkGrouping("priority")
        }
        step("Проверяем наличие заголовков групп") { mainPage.testCaseTable.checkGroupsPresent() }
        step("Проверяем новую группировку и отсутствие заголовков прежней") {
            mainPage.testCaseTable.checkGroupVisible("priority", "Critical")
            mainPage.testCaseTable.checkGroupDisappeared("category", fixture.categoryAlpha)
        }
    }

    @Test
    @AllureId("323")
    @DisplayName("Группировка применяется после фильтрации и не создаёт пустые группы")
    fun groupingUsesFilteredRowsOnly() {
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
        step("Группируем отфильтрованные тест-кейсы по статусу готовности") {
            mainPage.tableViewToolbar.groupBy("generalStatus")
        }
        step("Проверяем группы статусов только для прошедших фильтр тест-кейсов") {
            mainPage.testCaseTable.checkGroupVisible("generalStatus", "Готово")
            mainPage.testCaseTable.checkGroupVisible("generalStatus", "Бэклог")
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.thirdId)
        }
    }
}
