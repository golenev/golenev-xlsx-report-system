package org.golenev.tests.ui.filters

import io.qameta.allure.AllureId
import org.golenev.ui.pages.mainPage
import org.golenev.utils.step
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
@DisplayName("UI: Excel-подобные фильтры колонок таблицы тест-кейсов")
class ColumnFiltersUiTest : FilterUiTestBase() {

    @Test
    @AllureId("313")
    @DisplayName("Каждая колонка таблицы предоставляет собственный фильтр")
    fun eachColumnProvidesFilter() {
        val fixture = filterFixture()
        step("Добавляем три тест-кейса: два в категории ${fixture.categoryAlpha} со статусами «Готово» и «Бэклог», один в категории ${fixture.categoryBeta} со статусом «Готово»; приоритеты «Critical», «Blocker» и «Medium»") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем главную страницу") {
            mainPage.open()
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        listOf(
            "testId",
            "category",
            "shortTitle",
            "issueLink",
            "readyDate",
            "generalStatus",
            "priority",
            "scenario",
            "notes",
            "regressionStatus",
        ).forEach { columnKey ->
            step("Проверяем кнопку фильтра колонки $columnKey") { mainPage.testCaseTable.checkColumnFilterButton(columnKey) }
        }
    }

    @Test
    @AllureId("314")
    @DisplayName("Текстовый фильтр Test ID оставляет только совпадающий тест-кейс")
    fun textFilterLeavesMatchingTestCase() {
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
            mainPage.testCaseTable.openColumnFilter("testId")
        }
        step("Проверяем открытие фильтра колонки") { mainPage.columnFilterPanel.checkOpened("testId") }
        step("Открываем таблицу и применяем текстовый фильтр по идентификатору тест-кейса") {
            mainPage.columnFilterPanel.setTextFilter("testId", fixture.secondId)
            mainPage.columnFilterPanel.applyColumnFilter("testId")
        }
        step("Проверяем закрытие панели фильтра") { mainPage.columnFilterPanel.checkClosed("testId") }
        step("Проверяем применение фильтра колонки") { mainPage.testCaseTable.checkColumnFilterActive("testId") }
        step("Проверяем маршрут тест-кейсов через отфильтрованное состояние таблицы") {
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.firstId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.thirdId)
        }
    }

    @Test
    @AllureId("315")
    @DisplayName("Несколько значений Priority объединяются по правилу OR")
    fun severalPriorityValuesUseOrCondition() {
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
        step("Проверяем, что отображаются тест-кейсы любого из выбранных приоритетов") {
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.thirdId)
        }
    }

    @Test
    @AllureId("316")
    @DisplayName("Фильтры разных колонок объединяются по правилу AND")
    fun filtersFromDifferentColumnsUseAndCondition() {
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
        step("Проверяем, что остаётся тест-кейс, одновременно соответствующий обоим фильтрам") {
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.secondId)
            mainPage.testCaseTable.checkRowDisappeared(fixture.thirdId)
        }
    }

    @Test
    @AllureId("317")
    @DisplayName("Пустой результат фильтра объясняется и восстанавливается общим сбросом")
    fun emptyFilterResultCanBeReset() {
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
            mainPage.testCaseTable.openColumnFilter("testId")
        }
        step("Проверяем открытие фильтра колонки") { mainPage.columnFilterPanel.checkOpened("testId") }
        step("Открываем таблицу и применяем фильтр без совпадений") {
            mainPage.columnFilterPanel.setTextFilter("testId", "ABSENT-${fixture.token}")
            mainPage.columnFilterPanel.applyColumnFilter("testId")
        }
        step("Проверяем закрытие панели фильтра") { mainPage.columnFilterPanel.checkClosed("testId") }
        step("Проверяем применение фильтра колонки") { mainPage.testCaseTable.checkColumnFilterActive("testId") }
        step("Проверяем понятное пустое состояние таблицы") {
            mainPage.testCaseTable.checkEmptyFilterResult()
        }
        step("Сбрасываем фильтры и проверяем возврат предварительно созданных тест-кейсов") {
            mainPage.tableViewToolbar.clearAllFilters()
        }
        step("Проверяем отсутствие активных фильтров") { mainPage.tableViewToolbar.checkActiveFiltersCount(0) }
        step("Сбрасываем фильтры и проверяем возврат предварительно созданных тест-кейсов") {
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowVisible(fixture.thirdId)
        }
    }
}
