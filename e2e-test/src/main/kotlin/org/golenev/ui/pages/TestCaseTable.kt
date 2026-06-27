package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.ScrollIntoViewOptions.Block.start
import com.codeborne.selenide.ScrollIntoViewOptions.instant
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.SelenideElement

/**
 * Component Object таблицы тест-кейсов на главной странице Test Report.
 */
class TestCaseTable {
    /** Корневой элемент таблицы, внутри которого ищутся строки и кнопки таблицы. */
    private val root: SelenideElement get() = `$`("[data-testid='test-report-table']")

    /** Кнопка Add Row, которая открывает draft-строку для создания нового тест-кейса. */
    private val addRowButton: SelenideElement get() = `$`("button[data-role='button'][data-action='add-row']")

    /** Ленивый Selenide-локатор draft-строки, которая появляется только после нажатия Add Row. */
    private val draftRowElement: SelenideElement get() = root.find("[data-testid='test-case-row'][data-state='draft']")

    /** Объект draft-строки таблицы; создаётся лениво, чтобы не требовать наличие строки до Add Row. */
    val draftRow: TestCaseRow get() = TestCaseRow(draftRowElement)

    /** Возвращает объект существующей строки по Test ID через операторный доступ table[testId]. */
    operator fun get(testId: String): TestCaseRow = row(testId)

    /** Возвращает объект существующей строки таблицы по Test ID. */
    fun row(testId: String): TestCaseRow = TestCaseRow(rowElementByTestId(testId))

    /** Проверяет, что кнопка Add Row недоступна, пока нельзя начать создание новой строки. */
    fun checkAddRowDisabled() {
        addRowButton
            .scrollIntoView(instant().block(start))
            .shouldBe(disabled.because("кнопка Add Row должна быть недоступна, пока форма создания строки не готова к сохранению"))
    }

    /** Проверяет, что кнопка Add Row доступна для начала создания тест-кейса. */
    fun checkAddRowEnabled() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса"))
    }

    /** Проверяет, что сохранённая строка тест-кейса с указанным Test ID отображается. */
    fun checkRowVisible(testId: String) {
        row(testId).checkVisible()
    }

    /** Проверяет, что строка тест-кейса с указанным Test ID исчезла со страницы. */
    fun checkRowDisappeared(testId: String) {
        row(testId).checkDisappeared()
    }

    /** Нажимает Add Row и проверяет, что на странице появилась draft-строка. */
    fun startNewRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса")).click()
        draftRow.checkVisibleAfterDraftCreation()
    }

    /** Находит Selenide-элемент существующей строки таблицы по Test ID внутри root таблицы. */
    fun rowElementByTestId(testId: String): SelenideElement =
        root.find("[data-testid='test-case-row'][data-test-case-id='$testId']")
}
