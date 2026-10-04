package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import org.golenev.ui.allure.name

/**
 * Component Object предупреждения о несохранённых изменениях модального редактора.
 *
 * Проверка диалога, отказ от сохранения, продолжение и проверка закрытия разделены по элементам.
 * Видимость или закрытие родительского редактора тест проверяет отдельным шагом.
 * Диалог не получает функции проверки родителя и не обращается к нему скрыто.
 */
class UnsavedChangesDialog {
    private val unsavedChangesDialog = `$`(".unsaved-confirm").name("Предупреждение о несохранённых изменениях.")
    private val discardUnsavedChangesButton = `$`(".unsaved-discard").name("Кнопка Не сохранять в предупреждении.")
    private val continueEditingButton = `$`(".unsaved-confirm .secondary-btn").name("Кнопка Продолжить редактирование в предупреждении.")

    /**
     * Проверяем предупреждение о несохранённых изменениях
     */
    fun checkUnsavedChangesWarning() {
        unsavedChangesDialog
            .shouldBe(visible.because("при попытке закрытия должно появиться предупреждение"))
            .shouldHave(text("Сохранить изменения?").because("предупреждение должно предлагать сохранить изменения"))
            .shouldHave(text("У вас есть несохранённые изменения.").because("предупреждение должно объяснять причину показа"))
    }

    /**
     * Отказываемся от сохранения изменений и проверяем закрытие модального редактора
     */
    fun discardUnsavedChanges() {
        discardUnsavedChangesButton.shouldBe(visible).click()
    }

    /**
     * Продолжаем редактирование и проверяем возвращение в модальный редактор
     */
    fun continueEditing() {
        continueEditingButton.shouldBe(visible).click()
    }

    /**
     * Проверяем закрытие предупреждения о несохранённых изменениях
     */
    fun checkClosed() {
        unsavedChangesDialog.shouldBe(disappear.because("после продолжения редактирования предупреждение должно закрыться"))
    }
}
