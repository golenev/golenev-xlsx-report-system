package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.CollectionCondition.sizeGreaterThan
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import io.qameta.allure.Step
import org.golenev.ui.allure.name
import org.golenev.utils.CENTER

/**
 * Component Object таблицы тест-кейсов.
 *
 * Таблица отвечает за просмотр сохранённых строк и открытие редактора. Создание и изменение
 * тест-кейса делегируются отдельному модальному редактору.
 */
class TestCaseTable(
    private val testCaseEditor: TestCaseEditorModal,
    private val columnFilterPanel: ColumnFilterPanel,
) {
    private val savedRows =
        `$$`("[data-testid='test-case-row']").name("Сохранённые строки тест-кейсов")
    private val emptyFilterResult =
        `$`("[data-testid='table-empty-result']").name("Сообщение об отсутствии строк по заданным фильтрам.")
    private val filterButtons = `$$`("[data-testid='table-filter-button']")
        .name("Кнопки фильтров колонок таблицы.")
    private val groupRows = `$$`("[data-testid='table-group-row']")
        .name("Группы строк таблицы.")

    /** Глобальный виджет управления regression run в заголовке таблицы. */
    val regressionWidget = RegressionWidget()

    @Step("Проверяем активное состояние фильтра колонки {columnKey}")
    fun checkColumnFilterActive(columnKey: String) {
        filterButtons.findBy(attribute("data-name", columnKey))
            .shouldHave(attribute("data-state", "active").because("фильтр должен быть применён"))
    }

    @Step("Проверяем неактивное состояние фильтра колонки {columnKey}")
    fun checkColumnFilterInactive(columnKey: String) {
        filterButtons.findBy(attribute("data-name", columnKey))
            .shouldHave(attribute("data-state", "inactive").because("фильтр должен быть удалён"))
    }

    @Step("Проверяем наличие заголовков групп")
    fun checkGroupsPresent() {
        groupRows.shouldHave(sizeGreaterThan(0).because("после выбора группировки должны отображаться заголовки групп"))
    }

    @Step("Проверяем отсутствие заголовков групп")
    fun checkGroupsAbsent() {
        groupRows.shouldHave(size(0).because("без группировки заголовки групп не должны отображаться"))
    }

    @Step("Прокручиваем таблицу к строке с Test ID {testId} и проверяем её отображение")
    operator fun get(testId: String): TestCaseTable = apply { checkRowVisible(testId) }

    @Step("В строке с Test ID {testId} выбираем Regress Run {status}")
    fun selectRegressionStatus(testId: String, status: String) {
        val row = savedRows.findBy(attribute("data-test-case-id", testId)).name("Строка тест-кейса $testId")
        row.shouldBe(visible)
        row.`$`("[data-testid='regress-run-button']")
            .name("Select Regress Run в строке тест-кейса.")
            .shouldBe(enabled.because("select Regress Run должен быть доступен во время регресса"))
            .selectOption(status)
    }

    @Step("Прокручиваем к строке с Test ID {testId} и проверяем её видимость")
    fun checkRowVisible(testId: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).name("Строка тест-кейса $testId")
            .scrollIntoView(CENTER)
            .shouldBe(visible.because("строка тест-кейса должна отображаться в таблице"))
    }

    @Step("Проверяем исчезновение строки с Test ID {testId}")
    fun checkRowDisappeared(testId: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).name("Строка тест-кейса $testId")
            .shouldBe(disappear.because("строка тест-кейса должна исчезнуть"))
    }

    @Step("Проверяем количество сохранённых строк таблицы: {expectedCount}")
    fun checkSavedRowsCount(expectedCount: Int) {
        savedRows.shouldHave(size(expectedCount).because("количество строк должно соответствовать ожидаемому"))
    }

    @Step("Проверяем наличие фильтра в каждой колонке таблицы: {columnKeys}")
    fun checkColumnFilterButtons(columnKeys: List<String>) {
        columnKeys.forEach { columnKey ->
            filterButtons.findBy(attribute("data-name", columnKey)).name("Кнопка фильтра колонки $columnKey.")
                .shouldBe(visible.because("кнопка фильтра должна отображаться в колонке $columnKey"))
                .shouldHave(attribute("data-state", "inactive"))
        }
    }

    @Step("Открываем фильтр колонки {columnKey}")
    fun openColumnFilter(columnKey: String) {
        filterButtons.findBy(attribute("data-name", columnKey)).name("Кнопка фильтра колонки $columnKey.").shouldBe(enabled).click()
        columnFilterPanel.checkOpened(columnKey)
    }

    @Step("Проверяем сообщение об отсутствии тест-кейсов по заданным фильтрам")
    fun checkEmptyFilterResult() {
        emptyFilterResult
            .shouldBe(visible.because("для пустого результата должно отображаться отдельное сообщение"))
            .shouldHave(text("По заданным фильтрам тест-кейсы не найдены"))
    }

    @Step("Проверяем группу {groupValue} для колонки {columnKey}")
    fun checkGroupVisible(columnKey: String, groupValue: String) {
        groupRows.filterBy(attribute("data-name", columnKey)).findBy(attribute("data-value", groupValue))
            .name("Группа $groupValue колонки $columnKey.")
            .shouldBe(visible.because("группа $groupValue должна отображаться после группировки"))
            .shouldHave(attribute("data-state", "expanded"))
    }

    @Step("Проверяем отсутствие группы {groupValue} для колонки {columnKey}")
    fun checkGroupDisappeared(columnKey: String, groupValue: String) {
        groupRows.filterBy(attribute("data-name", columnKey)).findBy(attribute("data-value", groupValue))
            .name("Группа $groupValue колонки $columnKey.")
            .shouldBe(disappear.because("группа $groupValue не должна отображаться"))
    }

    @Step("Сворачиваем группу {groupValue} колонки {columnKey}")
    fun collapseGroup(columnKey: String, groupValue: String) {
        groupRows.filterBy(attribute("data-name", columnKey)).findBy(attribute("data-value", groupValue))
            .name("Группа $groupValue колонки $columnKey.").`$`("[data-testid='table-group-toggle']")
            .name("Кнопка сворачивания группы $groupValue.")
            .shouldBe(enabled)
            .click()
        groupRows.filterBy(attribute("data-name", columnKey)).findBy(attribute("data-value", groupValue))
            .name("Группа $groupValue колонки $columnKey.").shouldHave(attribute("data-state", "collapsed"))
    }

    @Step("Проверяем доступность редактирования Regress Run для тест-кейса {testId}")
    fun checkRegressionStatusEditable(testId: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).`$`("[data-testid='regress-run-button']")
            .name("Select Regress Run в строке тест-кейса $testId.")
            .shouldBe(enabled.because("фильтрация не должна блокировать редактирование Regress Run"))
    }

    @Step("Проверяем Ready Date {expectedDate} в строке с Test ID {testId}")
    fun checkReadyDate(testId: String, expectedDate: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).`$`("[data-name='Ready Date']")
            .name("Ячейка Ready Date в строке тест-кейса.")
            .shouldHave(text(expectedDate).because("Ready Date должна содержать ожидаемую дату"))
    }

    @Step("Открываем модальный редактор тест-кейса {testId}")
    fun openEditor(testId: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).`$`("[data-testid='scenario-edit']")
            .name("Кнопка изменения тест-кейса $testId.")
            .shouldBe(visible.because("кнопка изменения должна быть видимой в строке"))
            .click()
        testCaseEditor.checkEditModeReady(testId)
    }
}
