package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.ScrollIntoViewOptions.Block.start
import com.codeborne.selenide.ScrollIntoViewOptions.instant
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement

class TestCaseTable {
    private val addRowButton: SelenideElement = `$`("button[data-role='button'][data-action='add-row']")
    private val draftRowElement: SelenideElement get() = `$`("[data-testid='test-case-row'][data-state='draft']")
    private val tableRowSelectorPattern = "[data-testid='test-case-row'][data-test-case-id='%s']"

    val draftRow: TestCaseRow get() = TestCaseRow(draftRowElement, isDraft = true)

    operator fun get(testId: String): TestCaseRow = row(testId)

    fun row(testId: String): TestCaseRow = TestCaseRow(rowElementByTestId(testId), isDraft = false)

    fun shouldDisableAddRow() {
        addRowButton
            .scrollIntoView(instant().block(start))
            .shouldBe(disabled.because("кнопка должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    fun shouldEnableAddRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса"))
    }

    fun startNewRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса")).click()
        draftRow.root.shouldBe(visible.because("после нажатия добавления должна появиться черновая строка"))
    }

    fun shouldHaveRowsCount(expectedCount: Int) {
        `$$`("[data-testid='test-case-row'][data-test-case-id]").shouldHave(size(expectedCount).because("количество строк таблицы должно соответствовать ожидаемому значению"))
    }

    fun rowElementByTestId(testId: String): SelenideElement = element(tableRowSelectorPattern.format(testId))
}
