package org.golenev.ui.pages

import com.codeborne.selenide.Condition.enabled
import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide.`$`
import org.golenev.ui.allure.name

/**
 * Component Object футера редактора: сохраняет данные и проверяет индикатор несохранённых изменений.
 *
 * Методы работают отдельно с кнопкой сохранения или индикатором dirty.
 * Закрытие редактора после сохранения тест проверяет отдельным шагом; callbacks конструктора не нужны.
 */
class TestCaseEditorFooter {
    private val saveButton = `$`("[data-testid='save-test-case-button']").name("Кнопка сохранения модального редактора.")
    private val dirtyStatus = `$`(".test-case-modal-dirty").name("Статус несохранённых изменений модального редактора.")

    /**
     * Сохраняем новый тест-кейс и проверяем закрытие модального редактора
     */
    fun saveNewTestCase() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей")).click()
    }

    /**
     * Сохраняем изменения тест-кейса и проверяем закрытие модального редактора
     */
    fun saveChanges() {
        saveButton.shouldBe(enabled.because("кнопка сохранения изменений должна быть доступна")).click()
    }

    /**
     * Проверяем статус изменений модального редактора: {expectedStatus}
     */
    fun checkDirtyStatus(expectedStatus: String) {
        dirtyStatus.shouldHave(text(expectedStatus).because("статус должен отражать наличие несохранённых изменений"))
    }
}
