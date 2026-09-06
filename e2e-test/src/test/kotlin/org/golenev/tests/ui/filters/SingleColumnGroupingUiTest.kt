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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и группируем тест-кейсы по Category") {
            mainPage.open()
            mainPage.testCaseTable.groupBy("category")
        }
        step("Проверяем группы обеих категорий предварительных данных") {
            mainPage.testCaseTable.checkGroupVisible("category", fixture.categoryAlpha)
            mainPage.testCaseTable.checkGroupVisible("category", fixture.categoryBeta)
        }
        step("Сворачиваем первую категорию и проверяем скрытие только её тест-кейсов") {
            mainPage.testCaseTable.collapseGroup("category", fixture.categoryAlpha)
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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и сначала группируем по Category") {
            mainPage.open()
            mainPage.testCaseTable.groupBy("category")
            mainPage.testCaseTable.checkGroupVisible("category", fixture.categoryAlpha)
        }
        step("Переключаем единственную группировку на Priority") {
            mainPage.testCaseTable.groupBy("priority")
        }
        step("Проверяем новую группировку и отсутствие заголовков прежней") {
            mainPage.testCaseTable.checkGrouping("priority")
            mainPage.testCaseTable.checkGroupVisible("priority", "Critical")
            mainPage.testCaseTable.checkGroupDisappeared("category", fixture.categoryAlpha)
        }
    }

    @Test
    @AllureId("323")
    @DisplayName("Группировка применяется после фильтрации и не создаёт пустые группы")
    fun groupingUsesFilteredRowsOnly() {
        val fixture = filterFixture()
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и оставляем фильтром одну Category") {
            mainPage.open()
            mainPage.testCaseTable.selectFilterValues("category", listOf(fixture.categoryAlpha))
            mainPage.testCaseTable.applyColumnFilter("category")
        }
        step("Группируем отфильтрованные тест-кейсы по General Test Status") {
            mainPage.testCaseTable.groupBy("generalStatus")
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
