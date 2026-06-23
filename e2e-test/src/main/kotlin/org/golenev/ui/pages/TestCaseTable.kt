package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.ScrollIntoViewOptions.Block.start
import com.codeborne.selenide.ScrollIntoViewOptions.instant
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement

/**
 * Component Object таблицы тест-кейсов на главной странице Test Report.
 */
class TestCaseTable {
    /** Кнопка Add Row, которая открывает draft-строку для создания нового тест-кейса. */
    private val addRowButton: SelenideElement = `$`("button[data-role='button'][data-action='add-row']")
    /** Ленивый Selenide-локатор draft-строки, которая появляется только после нажатия Add Row. */
    private val draftRowElement: SelenideElement get() = `$`("[data-testid='test-case-row'][data-state='draft']")
    /** CSS-шаблон поиска существующей строки таблицы по значению data-test-case-id. */
    private val tableRowSelectorPattern = "[data-testid='test-case-row'][data-test-case-id='%s']"

    /** Объект draft-строки таблицы; создаётся лениво, чтобы не требовать наличие строки до Add Row. */
    val draftRow: TestCaseRow get() = TestCaseRow(draftRowElement, isDraft = true)

    /** Возвращает объект существующей строки по Test ID через операторный доступ table[testId]. */
    operator fun get(testId: String): TestCaseRow = row(testId)

    /** Возвращает объект существующей строки таблицы по Test ID. */
    fun row(testId: String): TestCaseRow = TestCaseRow(rowElementByTestId(testId), isDraft = false)

    /** Проверяет, что кнопка Add Row недоступна, пока нельзя начать создание новой строки. */
    fun shouldDisableAddRow() {
        addRowButton
            .scrollIntoView(instant().block(start))
            .shouldBe(disabled.because("кнопка должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    /** Проверяет, что кнопка Add Row доступна для начала создания тест-кейса. */
    fun shouldEnableAddRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса"))
    }

    /** Нажимает Add Row и проверяет, что на странице появилась draft-строка. */
    fun startNewRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса")).click()
        draftRow.root.shouldBe(visible.because("после нажатия добавления должна появиться черновая строка"))
    }

    /** Проверяет количество сохранённых строк тест-кейсов в таблице. */
    fun shouldHaveRowsCount(expectedCount: Int) {
        `$$`("[data-testid='test-case-row'][data-test-case-id]").shouldHave(size(expectedCount).because("количество строк таблицы должно соответствовать ожидаемому значению"))
    }

    /** Находит Selenide-элемент существующей строки таблицы по Test ID. */
    fun rowElementByTestId(testId: String): SelenideElement = element(tableRowSelectorPattern.format(testId))
}
