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
        step("Удаляем созданный запуск регрессионного тестирования") {
            RegressionDao.deleteByReleaseName(releaseName)
        }
    }

    @Test
    @AllureId("327")
    @DisplayName("Фильтр Regress Run сохраняет внешнее редактирование и пересчитывается после применения")
    fun regressionFilterKeepsInlineEditingAvailableUntilReapply() {
        val fixture = filterFixture()
        step("Добавляем подготовленные тест-кейсы") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем таблицу и запускаем регрессионное тестирование") {
            mainPage.open()
            mainPage.regressionWidget.startRegression(releaseName)
        }
        step("Устанавливаем первому тест-кейсу результат «Пройден» вне модального редактора") {
            mainPage.testCaseTable.selectRegressionStatus(fixture.firstId, "PASSED")
        }
        step("Применяем фильтр результатов регрессионного тестирования по значению «Пройден»") {
            mainPage.testCaseTable.openColumnFilter("regressionStatus")
            mainPage.testCaseTable.selectFilterValues("regressionStatus", listOf("PASSED"))
            mainPage.testCaseTable.applyColumnFilter("regressionStatus")
        }
        step("Проверяем доступность изменения результата регрессионного тестирования в отфильтрованной строке") {
            mainPage.testCaseTable.checkRegressionStatusEditable(fixture.firstId)
            mainPage.testCaseTable.selectRegressionStatus(fixture.firstId, "FAILED")
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
        }
        step("Повторно применяем фильтр и проверяем переход строки из отфильтрованного состояния") {
            mainPage.testCaseTable.openColumnFilter("regressionStatus")
            mainPage.testCaseTable.applyColumnFilter("regressionStatus")
            mainPage.testCaseTable.checkRowDisappeared(fixture.firstId)
        }
        step("Отменяем регрессионное тестирование после проверки") {
            mainPage.regressionWidget.cancelRegression()
        }
    }
}
