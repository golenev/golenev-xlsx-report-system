package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.Selenide.actions
import io.qameta.allure.Step
import org.golenev.ui.allure.name
import org.golenev.utils.typeOf
import org.openqa.selenium.Keys

/**
 * Component Object черновика фильтра: вводит условия, применяет их или закрывает панель.
 *
 * Конструктор получает одну функцию проверки результата вместо полного объекта таблицы.
 * Функция вызывается после применения и закрытия панели; аргумент — ключ выбранной колонки.
 * Передача функции при создании компонента ничего не проверяет и не открывает.
 * В MainPage функция делегирует проверку таблице: логическая зависимость от таблицы сохраняется,
 * хотя прямой ссылки на неё и доступа ко всем её действиям у панели нет.
 *
 * @param checkFilterApplied проверка активного состояния кнопки фильтра для переданного ключа колонки.
 */
class ColumnFilterPanel(private val checkFilterApplied: (String) -> Unit) {
    private val filterSearchInputs = `$$`("[data-testid='table-filter-search']")
        .name("Поля поиска фильтров колонок.")
    private val filterOptions = `$$`("[data-testid='table-filter-option']")
        .name("Значения фильтров колонок.")
    private val applyFilterButtons = `$$`("[data-action='apply-filter']")
        .name("Кнопки применения фильтров колонок.")
    private val filterPanels = `$$`("[data-testid='table-filter-panel']")
        .name("Панели фильтров колонок таблицы.")

    @Step("Проверяем открытие фильтра колонки {columnKey}")
    fun checkOpened(columnKey: String) {
        filterPanels.findBy(attribute("data-name", columnKey))
            .name("Панель фильтра колонки $columnKey.")
            .shouldBe(visible.because("панель выбранной колонки должна открыться"))
    }

    @Step("Задаём текстовый фильтр колонки {columnKey}: {query}")
    fun setTextFilter(columnKey: String, query: String) {
        filterSearchInputs.findBy(attribute("data-name", columnKey))
            .name("Поле текстового фильтра колонки $columnKey.")
            .shouldBe(visible)
            .typeOf(query)
    }

    @Step("Выбираем значения фильтра колонки {columnKey}: {values}")
    fun selectFilterValues(columnKey: String, values: List<String>) {
        values.forEach { value ->
            filterOptions.filterBy(attribute("data-name", columnKey))
                .findBy(attribute("data-value", value)).`$`("input")
                .name("Значение $value фильтра колонки $columnKey.")
                .shouldBe(enabled.because("значение фильтра должно быть доступно для выбора"))
                .click()
        }
    }

    @Step("Применяем фильтр колонки {columnKey}")
    fun applyColumnFilter(columnKey: String) {
        applyFilterButtons.findBy(attribute("data-name", columnKey))
            .name("Кнопка применения фильтра колонки $columnKey.")
            .shouldBe(enabled)
            .click()
        filterPanels.findBy(attribute("data-name", columnKey))
            .name("Панель фильтра колонки $columnKey.").shouldBe(disappear.because("после применения панель фильтра должна закрыться"))
        checkFilterApplied(columnKey)
    }

    @Step("Закрываем фильтр колонки {columnKey} без применения")
    fun closeColumnFilterWithoutApplying(columnKey: String) {
        actions().sendKeys(Keys.ESCAPE).perform()
        filterPanels.findBy(attribute("data-name", columnKey))
            .name("Панель фильтра колонки $columnKey.").shouldBe(disappear.because("панель фильтра должна закрыться без применения черновика"))
    }
}
