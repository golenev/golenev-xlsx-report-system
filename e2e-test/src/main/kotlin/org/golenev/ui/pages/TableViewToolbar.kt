package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import org.golenev.ui.allure.name

/**
 * Component Object управления группировкой и активными фильтрами представления таблицы.
 *
 * Методы отдельно работают с выбранным чипом, кнопкой общего сброса или select группировки.
 * Количество чипов, состояние кнопок колонок и наличие групп тест проверяет отдельными шагами.
 * Зависимость от таблицы в конструкторе не требуется.
 */
class TableViewToolbar {
    private val activeFilterChips =
        `$$`("[data-testid='active-filter-chip']").name("Чипы активных фильтров таблицы")
    private val clearAllFiltersButton =
        `$`("[data-action='clear-all-filters']").name("Кнопка сброса всех активных фильтров.")
    private val groupingSelect =
        `$`("[data-testid='table-group-select']").name("Select группировки строк таблицы.")

    /**
     * Проверяем активный фильтр колонки {columnKey} с описанием {expectedDescription}
     */
    fun checkActiveFilter(columnKey: String, expectedDescription: String) {
        activeFilterChips.findBy(attribute("data-name", columnKey))
            .name("Чип активного фильтра колонки $columnKey.")
            .shouldBe(visible)
            .shouldHave(text(expectedDescription))
    }

    /**
     * Проверяем количество активных фильтров: {expectedCount}
     */
    fun checkActiveFiltersCount(expectedCount: Int) {
        activeFilterChips.shouldHave(size(expectedCount).because("количество чипов должно совпадать с количеством активных фильтров"))
    }

    /**
     * Удаляем активный фильтр колонки {columnKey}
     */
    fun removeActiveFilter(columnKey: String) {
        activeFilterChips.findBy(attribute("data-name", columnKey))
            .name("Чип активного фильтра колонки $columnKey.")
            .shouldBe(enabled)
            .click()
    }

    /**
     * Сбрасываем все активные фильтры
     */
    fun clearAllFilters() {
        clearAllFiltersButton.shouldBe(enabled).click()
    }

    /**
     * Группируем строки таблицы по колонке {columnKey}
     */
    fun groupBy(columnKey: String) {
        groupingSelect.shouldBe(visible).selectOptionByValue(columnKey)
        groupingSelect.shouldHave(value(columnKey))
    }

    /**
     * Проверяем, что группировка выбрана по колонке {columnKey}
     */
    fun checkGrouping(columnKey: String) {
        groupingSelect.shouldHave(value(columnKey))
    }

    /**
     * Проверяем, что группировка не выбрана
     */
    fun checkGroupingInactive() {
        groupingSelect.shouldHave(exactValue(""))
    }
}
