package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import org.golenev.ui.allure.name
import org.golenev.utils.typeOf

/**
 * Component Object ввода структурированного сценария и вложений внутри модального редактора.
 *
 * Заполняем шаги и вложения structured scenario в модальном редакторе
 * Раскрываем шаг {stepIndex}, добавляем вложение и вводим его содержимое
 *
 * Составной маршрут выполняется в тесте. Каждый метод компонента работает с одним элементом:
 * кнопкой добавления, блоком шага, полем текста, toggle или полем вложения.
 * Элементы выбранного шага ищутся в объявленной коллекции по его пути; модалка скрыто не открывается.
 */
class ScenarioEditor {
    private val scenarioRootAddButton = `$`("[data-testid='scenario-root-add']")
        .name("Кнопка добавления корневого шага.")
    private val scenarioSteps = `$$`("[data-testid='scenario-editor-step']")
        .name("Шаги detailed scenario в модальном редакторе.")

    /**
     * Вводим простой Detailed Scenario в первый корневой шаг модального редактора
     */
    fun fillDetailedScenario(scenario: String) {
        scenarioSteps.findBy(attribute("data-scenario-path", "0")).`$`("[data-testid='scenario-step-input']")
            .shouldBe(visible.because("поле первого шага должно быть видимым для ввода сценария"))
            .typeOf(scenario)
    }

    /**
     * Добавляем корневой шаг сценария
     */
    fun addRootStep() {
        scenarioRootAddButton.shouldBe(enabled.because("добавление корневого шага должно быть доступно")).click()
    }

    /**
     * Проверяем отображение шага {stepNumber} сценария
     */
    fun checkStepVisible(stepIndex: Int, stepNumber: Int?) {
        scenarioSteps.findBy(attribute("data-scenario-path", stepIndex.toString()))
            .name("Блок шага $stepNumber detailed scenario.")
            .shouldBe(visible.because("блок шага $stepNumber должен быть видимым в модальном редакторе"))
    }

    /**
     * Вводим текст шага {stepIndex} сценария
     */
    fun fillStepText(stepIndex: Int, text: String) {
        scenarioSteps.findBy(attribute("data-scenario-path", stepIndex.toString())).`$`("[data-testid='scenario-step-input']")
            .name("Поле текста шага detailed scenario.")
            .shouldBe(visible.because("поле текста шага должно быть видимым"))
            .typeOf(text)
    }

    /**
     * Раскрываем шаг {stepIndex}
     */
    fun expandStep(stepIndex: Int) {
        scenarioSteps.findBy(attribute("data-scenario-path", stepIndex.toString())).`$`("[data-testid='scenario-step-toggle']")
            .name("Раскрываем шаг {stepIndex}")
            .shouldBe(visible.because("элемент шага должен быть видимым перед действием"))
            .click()
    }

    /**
     * Добавляем вложение шага {stepIndex}
     */
    fun addStepAttachment(stepIndex: Int) {
        scenarioSteps.findBy(attribute("data-scenario-path", stepIndex.toString())).`$`("[data-testid='scenario-attachment-add-button']")
            .name("Добавляем вложение шага {stepIndex}")
            .shouldBe(visible.because("элемент шага должен быть видимым перед действием"))
            .click()
    }

    /**
     * Вводим имя вложения шага {stepIndex}
     */
    fun fillAttachmentName(stepIndex: Int, attachmentName: String) {
        scenarioSteps.findBy(attribute("data-scenario-path", stepIndex.toString()))
            .`$`("[data-testid='scenario-editor-attachment'] .scenario-attachment-heading")
            .name("Поле имени вложения.")
            .shouldBe(visible.because("имя вложения должно редактироваться в заголовке"))
            .typeOf(attachmentName)
            .shouldHave(value(attachmentName).because("имя вложения должно сохраняться до общего сохранения"))
    }

    /**
     * Раскрываем вложение шага {stepIndex}
     */
    fun expandAttachment(stepIndex: Int) {
        scenarioSteps.findBy(attribute("data-scenario-path", stepIndex.toString()))
            .`$`("[data-testid='scenario-editor-attachment'] [data-testid='scenario-attachment-toggle']")
            .name("Кнопка раскрытия вложения.")
            .shouldBe(visible.because("кнопка раскрытия вложения должна быть видимой"))
            .click()
    }

    /**
     * Вводим содержимое вложения шага {stepIndex}
     */
    fun fillAttachmentContent(stepIndex: Int, attachmentContent: String) {
        scenarioSteps.findBy(attribute("data-scenario-path", stepIndex.toString()))
            .`$`("[data-testid='scenario-editor-attachment'] [data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения.")
            .shouldBe(visible.because("поле содержимого должно быть видимым после раскрытия вложения"))
            .typeOf(attachmentContent)
            .shouldHave(value(attachmentContent).because("содержимое вложения должно сохраняться до общего сохранения"))
    }
}
