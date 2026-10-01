package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.*
import io.qameta.allure.Step
import org.golenev.ui.allure.name
import org.golenev.utils.shouldBeVisibleForInput
import org.golenev.utils.typeOf
import org.openqa.selenium.Keys

class TestCaseEditorModal {
    private val editorLocator = "[data-testid='test-case-editor-modal']"
    private val editor =
        `$`(editorLocator).name("Модальный редактор создания или изменения тест-кейса.")
    private val testIdInput = `$`("[data-testid='test-id-input']").name("Поле Test ID в модальном редакторе.")
    private val categoryInput = `$`("[data-testid='category-input']").name("Поле Category / Feature в модальном редакторе.")
    private val shortTitleInput = `$`("[data-testid='short-title-input']").name("Поле Short Title в модальном редакторе.")
    private val issueLinkInput = `$`("[data-testid='youtrack-link']").name("Поле YouTrack Issue Link в модальном редакторе.")
    private val generalStatusSelect = `$`("$editorLocator [data-testid='status-dropdown']").name("Select General Test Status в модальном редакторе.")
    private val prioritySelect = `$`("$editorLocator [data-testid='priority-select']").name("Select Priority в модальном редакторе.")
    private val notesTextarea = `$`("[data-testid='notes-input']").name("Поле Notes в модальном редакторе.")
    private val readyDateInput = `$`("[data-testid='ready-date-input']").name("Поле Ready Date в модальном редакторе.")
    private val closeButton = `$`(".test-case-modal-close").name("Крестик закрытия модального редактора.")
    private val backdrop = `$`(".test-case-modal-backdrop").name("Область вне модального окна.")

    val scenarioEditor by lazy { ScenarioEditor() }
    val footer by lazy { TestCaseEditorFooter(::checkEditorClosed) }
    val unsavedChangesDialog by lazy { UnsavedChangesDialog(::checkVisible, ::checkEditorClosed) }

    @Step("Проверяем видимость модального редактора")
    fun checkVisible() {
        editor.shouldBe(visible.because("модальный редактор должен оставаться открытым"))
    }

    @Step("Проверяем готовность редактора создания")
    fun checkCreateModeReady() {
        checkVisible()
        testIdInput.shouldBe(enabled.because("в режиме создания Test ID должен быть доступен"))
    }

    @Step("Проверяем готовность редактора изменения тест-кейса {testId}")
    fun checkEditModeReady(testId: String) {
        checkVisible()
        testIdInput.shouldBe(disabled.because("в режиме изменения Test ID не должен редактироваться"))
            .shouldHave(value(testId).because("редактор должен открыться для выбранного тест-кейса"))
    }

    @Step("Вводим Test ID {testId} в модальном редакторе")
    fun fillTestId(testId: String) {
        testIdInput.shouldBeVisibleForInput("Test ID").typeOf(testId)
    }

    @Step("Вводим Category / Feature {category} в модальном редакторе")
    fun fillCategory(category: String) {
        categoryInput.shouldBeVisibleForInput("Category").typeOf(category)
    }

    @Step("Вводим Short Title {shortTitle} в модальном редакторе")
    fun fillShortTitle(shortTitle: String) {
        shortTitleInput.shouldBeVisibleForInput("Short Title").typeOf(shortTitle)
    }

    @Step("Вводим YouTrack Issue Link {issueLink} в модальном редакторе")
    fun fillIssueLink(issueLink: String) {
        issueLinkInput.shouldBeVisibleForInput("Issue Link").typeOf(issueLink)
    }

    @Step("Выбираем General Test Status {status} в модальном редакторе")
    fun selectGeneralStatus(status: String) {
        generalStatusSelect
            .shouldBe(visible.because("select статуса должен быть видимым в модальном редакторе"))
            .selectOption(status)
    }

    @Step("Выбираем Priority {priority} в модальном редакторе")
    fun selectPriority(priority: String) {
        prioritySelect
            .shouldBe(visible.because("select приоритета должен быть видимым в модальном редакторе"))
            .selectOption(priority)
    }

    @Step("Вводим Notes в модальном редакторе")
    fun fillNotes(notes: String) {
        notesTextarea.shouldBeVisibleForInput("Notes").typeOf(notes)
    }

    @Step("Устанавливаем Category / Feature {newValue} в открытом модальном редакторе")
    fun updateCategory(newValue: String) {
        categoryInput.shouldBeVisibleForInput("Category").setValue(newValue)
    }

    @Step("Закрываем модальный редактор клавишей Esc")
    fun closeEditorByEscape() {
        actions().sendKeys(Keys.ESCAPE).perform()
    }

    @Step("Закрываем модальный редактор крестиком")
    fun closeEditorByCloseButton() {
        closeButton.shouldBe(visible).click()
    }

    @Step("Закрываем модальный редактор нажатием вне модального окна")
    fun closeEditorByBackdropClick() {
        backdrop.shouldBe(visible)
        val leftVisibleAreaOffset = -(backdrop.size.width / 2) + 10
        actions().moveToElement(backdrop, leftVisibleAreaOffset, 0).click().perform()
    }

    @Step("Проверяем, что модальный редактор закрыт")
    fun checkEditorClosed() {
        editor.shouldBe(disappear.because("модальный редактор должен быть закрыт"))
    }

    @Step("Проверяем значение Category / Feature: {expectedCategory}")
    fun checkCategoryValue(expectedCategory: String) {
        categoryInput.shouldHave(value(expectedCategory).because("поле Category / Feature должно оставаться доступным для редактирования"))
    }

    @Step("Проверяем Ready Date в модальном редакторе: {expectedDate}")
    fun checkEditorReadyDate(expectedDate: String) {
        readyDateInput.shouldHave(value(expectedDate).because("Ready Date должна содержать ожидаемую дату"))
    }
}
