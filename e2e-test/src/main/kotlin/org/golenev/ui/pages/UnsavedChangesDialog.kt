package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import io.qameta.allure.Step
import org.golenev.ui.allure.name

/**
 * Component Object предупреждения о несохранённых изменениях модального редактора.
 *
 * Конструктор получает две функции, поскольку пользователь может выбрать два разных исхода:
 * продолжить редактирование с открытой модалкой или отказаться от изменений и закрыть её.
 * Функции проверяют соответствующий результат внутри framework-слоя без доступа диалога
 * к остальным действиям редактора. Ссылки на методы не выполняются при создании компонента.
 *
 * @param checkEditorVisible проверка видимости редактора при показе предупреждения и после продолжения.
 * @param checkEditorClosed ожидание закрытия редактора после отказа от сохранения.
 */
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
