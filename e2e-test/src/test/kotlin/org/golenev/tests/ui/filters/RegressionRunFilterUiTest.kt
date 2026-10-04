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
        step("Добавляем три тест-кейса: два в категории ${fixture.categoryAlpha} со статусами «Готово» и «Бэклог», один в категории ${fixture.categoryBeta} со статусом «Готово»; приоритеты «Critical», «Blocker» и «Medium»") {
            createPreliminaryTestDataViaApi(fixture)
        }
        step("Открываем главную страницу") {
            mainPage.open()
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Открываем форму запуска регресса") { mainPage.testCaseTable.regressionWidget.openStartForm() }
        step("Проверяем доступность ввода имени релиза") { mainPage.testCaseTable.regressionWidget.checkReleaseNameInputVisible() }
        step("Указываем имя релиза") { mainPage.testCaseTable.regressionWidget.fillReleaseName(releaseName) }
        step("Сохраняем запуск регресса") { mainPage.testCaseTable.regressionWidget.saveRegressionStart() }
        step("Проверяем запуск регресса") { mainPage.testCaseTable.regressionWidget.checkRegressionStarted() }
        step("Проверяем видимость строки тест-кейса") { mainPage.testCaseTable.checkRowVisible(fixture.firstId) }
        step("Устанавливаем первому тест-кейсу результат «Пройден» вне модального редактора") {
            mainPage.testCaseTable.selectRegressionStatus(fixture.firstId, "PASSED")
        }
        step("Открываем фильтр колонки") {
            mainPage.testCaseTable.openColumnFilter("regressionStatus")
        }
        step("Проверяем открытие фильтра колонки") { mainPage.columnFilterPanel.checkOpened("regressionStatus") }
        listOf("PASSED").forEach { filterValue ->
            step("Выбираем значение фильтра $filterValue") { mainPage.columnFilterPanel.selectFilterValue("regressionStatus", filterValue) }
        }
        step("Применяем фильтр колонки") {
            mainPage.columnFilterPanel.applyColumnFilter("regressionStatus")
        }
        step("Проверяем закрытие панели фильтра") { mainPage.columnFilterPanel.checkClosed("regressionStatus") }
        step("Проверяем применение фильтра колонки") { mainPage.testCaseTable.checkColumnFilterActive("regressionStatus") }
        step("Проверяем доступность изменения результата регрессионного тестирования в отфильтрованной строке") {
            mainPage.testCaseTable.checkRegressionStatusEditable(fixture.firstId)
        }
        step("Проверяем видимость строки тест-кейса") { mainPage.testCaseTable.checkRowVisible(fixture.firstId) }
        step("Проверяем доступность изменения результата регрессионного тестирования в отфильтрованной строке") {
            mainPage.testCaseTable.selectRegressionStatus(fixture.firstId, "FAILED")
            mainPage.testCaseTable.checkRowVisible(fixture.firstId)
        }
        step("Открываем фильтр колонки") {
            mainPage.testCaseTable.openColumnFilter("regressionStatus")
        }
        step("Проверяем открытие фильтра колонки") { mainPage.columnFilterPanel.checkOpened("regressionStatus") }
        step("Применяем фильтр колонки") {
            mainPage.columnFilterPanel.applyColumnFilter("regressionStatus")
        }
        step("Проверяем закрытие панели фильтра") { mainPage.columnFilterPanel.checkClosed("regressionStatus") }
        step("Проверяем применение фильтра колонки") { mainPage.testCaseTable.checkColumnFilterActive("regressionStatus") }
        step("Проверяем отсутствие строки тест-кейса") {
            mainPage.testCaseTable.checkRowDisappeared(fixture.firstId)
        }
        step("Отменяем регрессионное тестирование после проверки") {
            mainPage.testCaseTable.regressionWidget.cancelRegression()
        }
    }
}
