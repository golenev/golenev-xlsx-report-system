package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.*
import io.qameta.allure.Step
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.ui.allure.name
import org.golenev.utils.typeOf

class ScenarioEditor {
    private val scenarioRootAddButton = `$`("[data-testid='scenario-root-add']")
        .name("Кнопка добавления корневого шага.")
    private val scenarioSteps = `$$`("[data-testid='scenario-editor-step']")
        .name("Шаги detailed scenario в модальном редакторе.")

    @Step("Вводим простой Detailed Scenario в первый корневой шаг модального редактора")
    fun fillDetailedScenario(scenario: String) {
        scenarioSteps.findBy(attribute("data-scenario-path", "0")).`$`("[data-testid='scenario-step-input']")
            .shouldBe(visible.because("поле первого шага должно быть видимым для ввода сценария"))
            .typeOf(scenario)
    }

    @Step("Заполняем шаги и вложения structured scenario в модальном редакторе")
    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) {
        steps.forEachIndexed { index, step ->
            if (index > 0) {
                scenarioRootAddButton.click()
            }
            scenarioSteps.findBy(attribute("data-scenario-path", index.toString()))
                .name("Блок шага ${step.number} detailed scenario.")
                .shouldBe(visible.because("блок шага ${step.number} должен быть видимым в модальном редакторе"))
            scenarioSteps.findBy(attribute("data-scenario-path", index.toString())).`$`("[data-testid='scenario-step-input']")
                .name("Поле текста шага ${step.number} detailed scenario.")
                .shouldBe(visible.because("поле текста шага ${step.number} должно быть видимым"))
                .typeOf(step.text)

            step.attachments.firstOrNull { it.content.isNotBlank() }?.let { attachment ->
                fillScenarioStepAttachment(index, attachment.name, attachment.content.trim())
            }
        }
    }

    @Step("Раскрываем шаг {stepIndex}, добавляем вложение и вводим его содержимое")
    private fun fillScenarioStepAttachment(stepIndex: Int, attachmentName: String, attachmentContent: String) {
        val step = scenarioSteps.findBy(attribute("data-scenario-path", stepIndex.toString()))
        step.`$`("[data-testid='scenario-step-toggle']")
            .name("Кнопка раскрытия шага.")
            .click()
        step.`$`("[data-testid='scenario-attachment-add-button']").name("Кнопка добавления вложения.").shouldBe(visible).click()
        val attachmentEditor = step.`$`("[data-testid='scenario-editor-attachment']").name("Редактор вложения шага.")
        attachmentEditor.`$`(".scenario-attachment-heading")
            .name("Поле имени вложения.")
            .shouldBe(visible.because("имя вложения должно редактироваться в заголовке"))
            .typeOf(attachmentName)
            .shouldHave(value(attachmentName).because("имя вложения должно сохраняться до общего сохранения"))
        attachmentEditor.`$`("[data-testid='scenario-attachment-toggle']")
            .name("Кнопка раскрытия вложения.")
            .click()
        attachmentEditor.`$`("[data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения.")
            .shouldBe(visible.because("поле содержимого должно быть видимым после раскрытия вложения"))
            .typeOf(attachmentContent)
            .shouldHave(value(attachmentContent).because("содержимое вложения должно сохраняться до общего сохранения"))
    }
}
