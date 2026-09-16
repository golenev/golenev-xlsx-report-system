package org.golenev.tests.ui.filters

import io.qameta.allure.AllureId
import org.golenev.db.tables.regression.RegressionDao
import org.golenev.ui.pages.mainPage
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
@DisplayName("UI: Фильтр результатов активного regression run")
class RegressionRunFilterUiTest : FilterUiTestBase() {
    private val releaseName = "filter-regression-${getRandomTestId()}"

    @AfterEach
    fun cleanRegressionData() {
        step("Удаляем созданный regression run из базы") {
            RegressionDao.deleteByReleaseName(releaseName)
        }
    }

    @Test
    @AllureId("327")
    @DisplayName("Фильтр Regress Run сохраняет внешнее редактирование и пересчитывается после применения")
    fun regressionFilterKeepsInlineEditingAvailableUntilReapply() {
        val fixture = filterFixture()
        step("Создаём предварительные тестовые данные через API ручку batch-загрузки") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и запускаем regression run") {
            mainPage.open()
            mainPage.regressionWidget.startRegression(releaseName)
        }
        step("Устанавливаем первому тест-кейсу результат PASSED вне модального редактора") {
            mainPage.testCaseTable.selectRegressionStatus(fixture.firstId, "PASSED")
        }
        step("Применяем фильтр Regress Run по значению PASSED") {
            mainPage.testCaseTable.selectFilterValues("regressionStatus", listOf("PASSED"))
            mainPage.testCaseTable.applyColumnFilter("regressionStatus")
        }
        step("Проверяем доступность изменения Regress Run в отфильтрованной строке") {
            mainPage.testCaseTable.checkRegressionStatusEditable(fixture.firstId)
            mainPage.testCaseTable.selectRegressionStatus(fixture.firstId, "FAILED")
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
        }
        step("Повторно применяем фильтр и проверяем переход строки из отфильтрованного состояния") {
            mainPage.testCaseTable.openColumnFilter("regressionStatus")
            mainPage.testCaseTable.applyColumnFilter("regressionStatus")
            mainPage.testCaseTable.checkRowDisappeared(fixture.firstId)
        }
        step("Отменяем regression run после проверки") {
            mainPage.regressionWidget.cancelRegression()
        }
    }
}
