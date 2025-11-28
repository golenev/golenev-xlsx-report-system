package com.example.e2e.ui.pages

import com.codeborne.selenide.CollectionCondition
import com.codeborne.selenide.Condition.disappear
import com.codeborne.selenide.Condition.enabled
import com.codeborne.selenide.Condition.hidden
import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.ElementsCollection
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.SelenideElement
import com.example.e2e.utils.step

class MainPage {

    private val headerTitle: SelenideElement = element("h1")
    private val addRowButton: SelenideElement = `$$`("button.secondary-btn").first()
    private val newRow: SelenideElement = element("tr.new-row")

    private val newRowInputs: ElementsCollection
        get() = newRow.$$("input.cell-input")

    private val newRowSaveButton: SelenideElement
        get() = newRow.find("button.save-btn")

    fun open() = step("Открываем главную страницу") {
        open("/")
        headerTitle.shouldHave(text("Test Report"))
    }

    fun startNewRow() = step("Нажимаем кнопку Add Row") {
        addRowButton.shouldBe(enabled).click()
        newRow.shouldBe(visible)
    }

    fun fillTestId(testId: String) = step("Заполняем поле Test ID значением $testId") {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1))
        newRowInputs.first().setValue(testId)
    }

    fun fillCategory(category: String) = step("Заполняем поле Category / Feature значением $category") {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(2))
        newRowInputs[1].setValue(category)
    }

    fun saveNewRow() = step("Сохраняем новую строку") {
        newRowSaveButton.shouldBe(enabled).click()
        newRow.shouldBe(hidden)
    }

    fun shouldSeeTestCase(testId: String) = step("Проверяем, что тест-кейс $testId отображается в таблице") {
        tableRowByTestId(testId).shouldBe(visible)
    }

    fun deleteTestCase(testId: String) = step("Удаляем тест-кейс $testId") {
        val row = tableRowByTestId(testId).shouldBe(visible)
        row.find("button.delete-btn").click()
    }

    fun shouldNotSeeTestCase(testId: String) = step("Проверяем, что тест-кейс $testId удалён из таблицы") {
        tableRowByTestId(testId).should(disappear)
    }

    private fun tableRowByTestId(testId: String): SelenideElement =
        element("tbody tr[data-test-id='tr-data-test-id-${testId}']")
}
