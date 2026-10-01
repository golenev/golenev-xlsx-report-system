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
            mainPage.testCaseTable.selectFilterValues("category", listOf(fixture.categoryAlpha))
            mainPage.testCaseTable.applyColumnFilter("category")
            mainPage.testCaseTable.openColumnFilter("generalStatus")
            mainPage.testCaseTable.selectFilterValues("generalStatus", listOf("Готово"))
            mainPage.testCaseTable.applyColumnFilter("generalStatus")
        }
        step("Проверяем два обозначения с понятными описаниями активных фильтров") {
            mainPage.testCaseTable.checkActiveFiltersCount(2)
            mainPage.testCaseTable.checkActiveFilter("category", fixture.categoryAlpha)
            mainPage.testCaseTable.checkActiveFilter("generalStatus", "Готово")
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
                testCaseTable.selectFilterValues("category", listOf(fixture.categoryAlpha))
                testCaseTable.applyColumnFilter("category")
                testCaseTable.openColumnFilter("generalStatus")
                testCaseTable.selectFilterValues("generalStatus", listOf("Готово"))
                testCaseTable.applyColumnFilter("generalStatus")
            }
        }
        step("Убираем только фильтр статуса") {
            mainPage.testCaseTable.removeActiveFilter("generalStatus")
        }
        step("Проверяем сохранение фильтра категории и возврат второго тест-кейса этой категории") {
            with(mainPage) {
                testCaseTable.checkActiveFiltersCount(1)
                testCaseTable.checkActiveFilter("category", fixture.categoryAlpha)
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
            mainPage.testCaseTable.selectFilterValues("category", listOf(fixture.categoryAlpha))
            mainPage.testCaseTable.applyColumnFilter("category")
            mainPage.testCaseTable.openColumnFilter("priority")
            mainPage.testCaseTable.selectFilterValues("priority", listOf("Critical"))
            mainPage.testCaseTable.applyColumnFilter("priority")
        }
        step("Сбрасываем все фильтры одной командой") {
            mainPage.testCaseTable.clearAllFilters()
        }
        step("Проверяем отсутствие фильтров и возврат всех предварительно созданных строк") {
            mainPage.testCaseTable.checkActiveFiltersCount(0)
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowVisible(fixture.thirdId)
            mainPage.checkUrlDoesNotContain(listOf("filter."))
        }
    }
}
