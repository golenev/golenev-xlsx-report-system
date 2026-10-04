package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.Selenide.actions
import org.golenev.ui.allure.name
import org.golenev.utils.typeOf
import org.openqa.selenium.Keys

/**
 * Component Object черновика фильтра: вводит условия, применяет их или закрывает панель.
 *
 * Каждый метод работает с одним выбранным элементом панели. Перебор значений остаётся в тесте.
 * Открытие и закрытие панели, а также активность кнопки фильтра тест проверяет явно отдельными шагами.
 * Обратная зависимость от таблицы и callback проверки применения отсутствуют.
 */
class ColumnFilterPanel {
    private val filterSearchInputs = `$$`("[data-testid='table-filter-search']")
        .name("Поля поиска фильтров колонок.")
    private val filterOptions = `$$`("[data-testid='table-filter-option']")
        .name("Значения фильтров колонок.")
    private val applyFilterButtons = `$$`("[data-action='apply-filter']")
        .name("Кнопки применения фильтров колонок.")
    private val filterPanels = `$$`("[data-testid='table-filter-panel']")
        .name("Панели фильтров колонок таблицы.")

    /**
     * Проверяем открытие фильтра колонки {columnKey}
     */
    fun checkOpened(columnKey: String) {
        filterPanels.findBy(attribute("data-name", columnKey))
            .name("Панель фильтра колонки $columnKey.")
            .shouldBe(visible.because("панель выбранной колонки должна открыться"))
    }

    /**
     * Задаём текстовый фильтр колонки {columnKey}: {query}
     */
    fun setTextFilter(columnKey: String, query: String) {
        filterSearchInputs.findBy(attribute("data-name", columnKey))
            .name("Поле текстового фильтра колонки $columnKey.")
            .shouldBe(visible)
            .typeOf(query)
    }

    /**
     * Выбираем значения фильтра колонки {columnKey}: {values}
     */
    fun selectFilterValue(columnKey: String, value: String) {
        filterOptions.filterBy(attribute("data-name", columnKey))
            .findBy(attribute("data-value", value)).`$`("input")
            .name("Значение $value фильтра колонки $columnKey.")
            .shouldBe(enabled.because("значение фильтра должно быть доступно для выбора"))
            .click()
    }

    /**
     * Применяем фильтр колонки {columnKey}
     */
    fun applyColumnFilter(columnKey: String) {
        applyFilterButtons.findBy(attribute("data-name", columnKey))
            .name("Кнопка применения фильтра колонки $columnKey.")
            .shouldBe(enabled)
            .click()
    }

    /**
     * Закрываем фильтр колонки {columnKey} без применения
     */
    fun closeColumnFilterWithoutApplying(columnKey: String) {
        actions().sendKeys(Keys.ESCAPE).perform()
    }

    /**
     * Проверяем закрытие фильтра колонки {columnKey}
     */
    fun checkClosed(columnKey: String) {
        filterPanels.findBy(attribute("data-name", columnKey))
            .name("Панель фильтра колонки $columnKey.")
            .shouldBe(disappear.because("панель фильтра должна закрыться после действия"))
    }
}
