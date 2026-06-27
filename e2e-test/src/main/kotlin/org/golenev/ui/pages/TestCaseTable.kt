package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.SelenideElement

/**
 * Component Object таблицы тест-кейсов на главной странице Test Report.
 */
class TestCaseTable {
    /** Корневой элемент таблицы, внутри которого ищутся строки и кнопки таблицы. */
    val root: SelenideElement get() = `$`("[data-testid='test-report-table']")

    /** Кнопка Add Row, которая открывает draft-строку для создания нового тест-кейса. */
    val addRowButton: SelenideElement get() = `$`("button[data-role='button'][data-action='add-row']")

    /** Ленивый Selenide-локатор draft-строки, которая появляется только после нажатия Add Row. */
    private val draftRowElement: SelenideElement get() = root.find("[data-testid='test-case-row'][data-state='draft']")

    /** Объект draft-строки таблицы; создаётся лениво, чтобы не требовать наличие строки до Add Row. */
    val draftRow: TestCaseRow get() = TestCaseRow(draftRowElement)

    /** Возвращает объект существующей строки по Test ID через операторный доступ table[testId]. */
    operator fun get(testId: String): TestCaseRow = row(testId)

    /** Возвращает объект существующей строки таблицы по Test ID. */
    fun row(testId: String): TestCaseRow = TestCaseRow(rowElementByTestId(testId))


    /** Нажимает Add Row и проверяет, что на странице появилась draft-строка. */
    fun startNewRow() {
        addRowButton.shouldBe(enabled.because("кнопка добавления строки должна быть доступна для начала создания тест-кейса")).click()
        draftRow.root.shouldBe(visible.because("после нажатия добавления должна появиться черновая строка"))
    }


    /** Находит Selenide-элемент существующей строки таблицы по Test ID внутри root таблицы. */
    fun rowElementByTestId(testId: String): SelenideElement =
        root.find("[data-testid='test-case-row'][data-test-case-id='$testId']")
}
