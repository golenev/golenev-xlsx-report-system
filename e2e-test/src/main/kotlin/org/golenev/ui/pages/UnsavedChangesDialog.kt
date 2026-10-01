package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.*
import io.qameta.allure.Step
import org.golenev.ui.allure.name

class UnsavedChangesDialog(
    private val checkEditorVisible: () -> Unit,
    private val checkEditorClosed: () -> Unit,
) {
    private val unsavedChangesDialog = `$`(".unsaved-confirm").name("Предупреждение о несохранённых изменениях.")
    private val discardUnsavedChangesButton = `$`(".unsaved-discard").name("Кнопка Не сохранять в предупреждении.")
    private val continueEditingButton = `$`(".unsaved-confirm .secondary-btn").name("Кнопка Продолжить редактирование в предупреждении.")

    @Step("Проверяем предупреждение о несохранённых изменениях")
    fun checkUnsavedChangesWarning() {
        checkEditorVisible()
        unsavedChangesDialog
            .shouldBe(visible.because("при попытке закрытия должно появиться предупреждение"))
            .shouldHave(text("Сохранить изменения?").because("предупреждение должно предлагать сохранить изменения"))
            .shouldHave(text("У вас есть несохранённые изменения.").because("предупреждение должно объяснять причину показа"))
    }

    @Step("Отказываемся от сохранения изменений и проверяем закрытие модального редактора")
    fun discardUnsavedChanges() {
        discardUnsavedChangesButton.shouldBe(visible).click()
        checkEditorClosed()
    }

    @Step("Продолжаем редактирование и проверяем возвращение в модальный редактор")
    fun continueEditing() {
        continueEditingButton.shouldBe(visible).click()
        unsavedChangesDialog.shouldBe(disappear.because("после продолжения редактирования предупреждение должно закрыться"))
        checkEditorVisible()
    }
}
