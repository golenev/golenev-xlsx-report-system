package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.*
import io.qameta.allure.Step
import org.golenev.ui.allure.name

class TestCaseEditorFooter(private val checkEditorClosed: () -> Unit) {
    private val saveButton = `$`("[data-testid='save-test-case-button']").name("Кнопка сохранения модального редактора.")
    private val dirtyStatus = `$`(".test-case-modal-dirty").name("Статус несохранённых изменений модального редактора.")

    @Step("Сохраняем новый тест-кейс и проверяем закрытие модального редактора")
    fun saveNewTestCase() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей")).click()
        checkEditorClosed()
    }

    @Step("Сохраняем изменения тест-кейса и проверяем закрытие модального редактора")
    fun saveChanges() {
        saveButton.shouldBe(enabled.because("кнопка сохранения изменений должна быть доступна")).click()
        checkEditorClosed()
    }

    @Step("Проверяем статус изменений модального редактора: {expectedStatus}")
    fun checkDirtyStatus(expectedStatus: String) {
        dirtyStatus.shouldHave(text(expectedStatus).because("статус должен отражать наличие несохранённых изменений"))
    }
}
