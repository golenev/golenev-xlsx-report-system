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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу, применяем фильтры и группировку") {
            mainPage.open()
            mainPage.testCaseTable.selectFilterValues("category", listOf(fixture.categoryAlpha))
            mainPage.testCaseTable.applyColumnFilter("category")
            mainPage.testCaseTable.selectFilterValues("priority", listOf("Critical", "Blocker"))
            mainPage.testCaseTable.applyColumnFilter("priority")
            mainPage.testCaseTable.groupBy("category")
        }
        step("Проверяем фиксацию представления таблицы в URL") {
            mainPage.checkUrlParameters(
                listOf(
                    "filter.category=${encodeQueryValue(fixture.categoryAlpha)}",
                    "filter.priority=Critical",
                    "filter.priority=Blocker",
                    "groupBy=category",
                ),
            )
        }
        step("Обновляем страницу и проверяем восстановление бизнес-состояния таблицы") {
            mainPage.refreshCurrentPage()
            mainPage.testCaseTable.checkActiveFiltersCount(2)
            mainPage.testCaseTable.checkGrouping("category")
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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
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
        step("Проверяем восстановленное по ссылке представление предварительных тест-кейсов") {
            mainPage.testCaseTable.checkActiveFiltersCount(2)
            mainPage.testCaseTable.checkActiveFilter("category", fixture.categoryAlpha)
            mainPage.testCaseTable.checkGrouping("category")
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
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем прямую ссылку с неизвестными фильтром, условием и группировкой") {
            mainPage.openWithQuery("filter.unknown=value&filter.testId.mode=wrong&groupBy=unknown")
        }
        step("Проверяем исходное представление и доступность предварительных тест-кейсов") {
            mainPage.testCaseTable.checkActiveFiltersCount(0)
            mainPage.testCaseTable.checkGroupingInactive()
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
            mainPage.testCaseTable.checkRowVisible(fixture.secondId)
            mainPage.testCaseTable.checkRowVisible(fixture.thirdId)
        }
    }
}
