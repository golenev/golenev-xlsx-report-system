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

    private val newRowTextAreas: ElementsCollection
        get() = newRow.$$("textarea.cell-textarea")

    private val generalStatusDropdown: SelenideElement
        get() = newRow.`$$`("div.status-dropdown").first()

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

    fun fillShortTitle(shortTitle: String) = step("Заполняем поле Short Title значением $shortTitle") {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(3))
        newRowInputs[2].setValue(shortTitle)
    }

    fun fillIssueLink(issueLink: String) = step("Заполняем поле YouTrack Issue Link значением $issueLink") {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(4))
        newRowInputs[3].setValue(issueLink)
    }

    fun fillReadyDate(readyDate: String) = step("Заполняем поле Ready Date значением $readyDate") {
        newRowInputs.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(5))
        newRowInputs[4].setValue(readyDate)
    }

    fun selectGeneralStatus(status: String) = step("Выбираем значение General Test Status: $status") {
        generalStatusDropdown.shouldBe(visible)
        generalStatusDropdown.find("summary").click()
        generalStatusDropdown.findAll("button.status-option").findBy(text(status)).click()
    }

    fun fillDetailedScenario(scenario: String) = step("Заполняем поле Detailed Scenario значением $scenario") {
        newRowTextAreas.shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1))
        newRowTextAreas.first().setValue(scenario)
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
