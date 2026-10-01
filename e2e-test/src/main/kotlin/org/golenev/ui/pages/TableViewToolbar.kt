package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.*
import io.qameta.allure.Step
import org.golenev.ui.allure.name

class TableViewToolbar(private val testCaseTable: TestCaseTable) {
    private val activeFilterChips =
        `$$`("[data-testid='active-filter-chip']").name("Чипы активных фильтров таблицы")
    private val clearAllFiltersButton =
        `$`("[data-action='clear-all-filters']").name("Кнопка сброса всех активных фильтров.")
    private val groupingSelect =
        `$`("[data-testid='table-group-select']").name("Select группировки строк таблицы.")

    @Step("Проверяем активный фильтр колонки {columnKey} с описанием {expectedDescription}")
    fun checkActiveFilter(columnKey: String, expectedDescription: String) {
        activeFilterChips.findBy(attribute("data-name", columnKey))
            .name("Чип активного фильтра колонки $columnKey.")
            .shouldBe(visible)
            .shouldHave(text(expectedDescription))
    }

    @Step("Проверяем количество активных фильтров: {expectedCount}")
    fun checkActiveFiltersCount(expectedCount: Int) {
        activeFilterChips.shouldHave(size(expectedCount).because("количество чипов должно совпадать с количеством активных фильтров"))
    }

    @Step("Удаляем активный фильтр колонки {columnKey}")
    fun removeActiveFilter(columnKey: String) {
        activeFilterChips.findBy(attribute("data-name", columnKey))
            .name("Чип активного фильтра колонки $columnKey.")
            .shouldBe(enabled)
            .click()
        testCaseTable.checkColumnFilterInactive(columnKey)
    }

    @Step("Сбрасываем все активные фильтры")
    fun clearAllFilters() {
        clearAllFiltersButton.shouldBe(enabled).click()
        activeFilterChips.shouldHave(size(0).because("после общего сброса активных фильтров не должно остаться"))
    }

    @Step("Группируем строки таблицы по колонке {columnKey}")
    fun groupBy(columnKey: String) {
        groupingSelect.shouldBe(visible).selectOptionByValue(columnKey)
        groupingSelect.shouldHave(value(columnKey))
    }

    @Step("Проверяем, что группировка выбрана по колонке {columnKey}")
    fun checkGrouping(columnKey: String) {
        groupingSelect.shouldHave(value(columnKey))
        testCaseTable.checkGroupsPresent()
    }

    @Step("Проверяем, что группировка не выбрана")
    fun checkGroupingInactive() {
        groupingSelect.shouldHave(exactValue(""))
        testCaseTable.checkGroupsAbsent()
    }
}
