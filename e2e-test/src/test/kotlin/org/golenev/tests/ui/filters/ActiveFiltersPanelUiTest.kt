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
        step("Добавляем подготовленные тест-кейсы") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и применяем два фильтра") {
            mainPage.open()
            mainPage.testCaseTable.openColumnFilter("category")
            mainPage.columnFilterPanel.selectFilterValues("category", listOf(fixture.categoryAlpha))
            mainPage.columnFilterPanel.applyColumnFilter("category")
            mainPage.testCaseTable.openColumnFilter("generalStatus")
            mainPage.columnFilterPanel.selectFilterValues("generalStatus", listOf("Готово"))
            mainPage.columnFilterPanel.applyColumnFilter("generalStatus")
        }
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
        step("Добавляем подготовленные тест-кейсы") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и ограничиваем строки категорией и статусом") {
            with(mainPage) {
                open()
                testCaseTable.openColumnFilter("category")
                columnFilterPanel.selectFilterValues("category", listOf(fixture.categoryAlpha))
                columnFilterPanel.applyColumnFilter("category")
                testCaseTable.openColumnFilter("generalStatus")
                columnFilterPanel.selectFilterValues("generalStatus", listOf("Готово"))
                columnFilterPanel.applyColumnFilter("generalStatus")
            }
        }
        step("Убираем только фильтр статуса") {
            mainPage.tableViewToolbar.removeActiveFilter("generalStatus")
        }
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
        step("Добавляем подготовленные тест-кейсы") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и применяем два фильтра") {
            mainPage.open()
            mainPage.testCaseTable.openColumnFilter("category")
            mainPage.columnFilterPanel.selectFilterValues("category", listOf(fixture.categoryAlpha))
            mainPage.columnFilterPanel.applyColumnFilter("category")
            mainPage.testCaseTable.openColumnFilter("priority")
            mainPage.columnFilterPanel.selectFilterValues("priority", listOf("Critical"))
            mainPage.columnFilterPanel.applyColumnFilter("priority")
        }
        step("Сбрасываем все фильтры одной командой") {
            mainPage.tableViewToolbar.clearAllFilters()
        }
        step("Проверяем отсутствие фильтров и возврат всех предварительно созданных строк") {
            mainPage.tableViewToolbar.checkActiveFiltersCount(0)
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowVisible(fixture.thirdId)
            mainPage.checkUrlDoesNotContain(listOf("filter."))
        }
    }
}
