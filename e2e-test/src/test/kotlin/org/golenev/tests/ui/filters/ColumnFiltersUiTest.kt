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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу тест-кейсов") {
            mainPage.open()
        }
        step("Проверяем наличие отдельной кнопки фильтра в каждой колонке") {
            mainPage.testCaseTable.checkColumnFilterButtons(
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
                ),
            )
        }
    }

    @Test
    @AllureId("314")
    @DisplayName("Текстовый фильтр Test ID оставляет только совпадающий тест-кейс")
    fun textFilterLeavesMatchingTestCase() {
        val fixture = filterFixture()
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и применяем текстовый фильтр по Test ID") {
            mainPage.open()
            mainPage.testCaseTable.setTextFilter("testId", fixture.secondId)
            mainPage.testCaseTable.applyColumnFilter("testId")
        }
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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и выбираем Critical и Blocker в фильтре Priority") {
            mainPage.open()
            mainPage.testCaseTable.selectFilterValues("priority", listOf("Critical", "Blocker"))
            mainPage.testCaseTable.applyColumnFilter("priority")
        }
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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и применяем фильтры Category и General Test Status") {
            mainPage.open()
            mainPage.testCaseTable.selectFilterValues("category", listOf(fixture.categoryAlpha))
            mainPage.testCaseTable.applyColumnFilter("category")
            mainPage.testCaseTable.selectFilterValues("generalStatus", listOf("Готово"))
            mainPage.testCaseTable.applyColumnFilter("generalStatus")
        }
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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и применяем фильтр без совпадений") {
            mainPage.open()
            mainPage.testCaseTable.setTextFilter("testId", "ABSENT-${fixture.token}")
            mainPage.testCaseTable.applyColumnFilter("testId")
        }
        step("Проверяем понятное пустое состояние таблицы") {
            mainPage.testCaseTable.checkEmptyFilterResult()
        }
        step("Сбрасываем фильтры и проверяем возврат предварительно созданных тест-кейсов") {
            mainPage.testCaseTable.clearAllFilters()
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowVisible(fixture.thirdId)
        }
    }
}
