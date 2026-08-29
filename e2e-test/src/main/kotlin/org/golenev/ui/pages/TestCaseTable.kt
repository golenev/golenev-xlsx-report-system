package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.ElementsCollection
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.Selenide.`$x`
import com.codeborne.selenide.Selenide.actions
import com.codeborne.selenide.SelenideElement
import io.qameta.allure.Step
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.ui.allure.name
import org.golenev.utils.CENTER
import org.golenev.utils.shouldBeVisibleForInput
import org.golenev.utils.typeOf
import org.openqa.selenium.Keys

/**
 * Component Object таблицы тест-кейсов и связанного с ней модального редактора.
 *
 * Таблица отвечает за просмотр сохранённых строк и открытие редактора. Создание и изменение
 * тест-кейса выполняются только внутри модального окна `test-case-editor-modal`.
 */
class TestCaseTable {
    private val tableLocator = "[data-testid='test-report-table']"
    private val editorLocator = "[data-testid='test-case-editor-modal']"

    private val addRowButton: SelenideElement =
        `$`("button[data-role='button'][data-action='add-row']")
            .name("Кнопка Add Row, которая открывает модальный редактор нового тест-кейса.")

    private val editor: SelenideElement =
        `$`(editorLocator).name("Модальный редактор создания или изменения тест-кейса.")

    private val testIdInput = `$`("$editorLocator [data-testid='test-id-input']").name("Поле Test ID в модальном редакторе.")
    private val categoryInput = modalFieldByLabel("Category / Feature", "textarea").name("Поле Category / Feature в модальном редакторе.")
    private val shortTitleInput = modalFieldByLabel("Short Title", "textarea").name("Поле Short Title в модальном редакторе.")
    private val issueLinkInput = `$`("$editorLocator [data-testid='youtrack-link']").name("Поле YouTrack Issue Link в модальном редакторе.")
    private val generalStatusSelect = `$`("$editorLocator select[data-testid='status-dropdown']").name("Select General Test Status в модальном редакторе.")
    private val prioritySelect = `$`("$editorLocator select[data-testid='priority-select']").name("Select Priority в модальном редакторе.")
    private val notesTextarea = `$`("$editorLocator [data-testid='notes-input']").name("Поле Notes в модальном редакторе.")
    private val saveButton = `$`("$editorLocator [data-testid='save-test-case-button']").name("Кнопка сохранения модального редактора.")
    private val readyDateInput = modalFieldByLabel("Ready Date", "input").name("Поле Ready Date в модальном редакторе.")
    private val dirtyStatus = `$`("$editorLocator .test-case-modal-dirty").name("Статус несохранённых изменений модального редактора.")
    private val closeButton = `$`("$editorLocator .test-case-modal-close").name("Крестик закрытия модального редактора.")
    private val backdrop = `$`("$editorLocator .test-case-modal-backdrop").name("Область вне модального окна.")
    private val unsavedChangesDialog = `$`("$editorLocator [role='alertdialog']").name("Предупреждение о несохранённых изменениях.")
    private val discardUnsavedChangesButton = `$`("$editorLocator .unsaved-discard").name("Кнопка Не сохранять в предупреждении.")
    private val continueEditingButton = `$`("$editorLocator [role='alertdialog'] .secondary-btn").name("Кнопка Продолжить редактирование в предупреждении.")

    private val savedRows: ElementsCollection =
        `$$`("$tableLocator [data-testid='test-case-row']").name("Сохранённые строки тест-кейсов")

    @Step("Прокручиваем таблицу к строке с Test ID {testId} и проверяем её отображение")
    operator fun get(testId: String): TestCaseTable = apply { checkRowVisible(testId) }

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

    @Step("Вводим простой Detailed Scenario в первый корневой шаг модального редактора")
    fun fillDetailedScenario(scenario: String) {
        scenarioStepInput(0)
            .shouldBe(visible.because("поле первого шага должно быть видимым для ввода сценария"))
            .typeOf(scenario)
    }

    @Step("Заполняем шаги и вложения structured scenario в модальном редакторе")
    fun fillDetailedScenarioSteps(steps: List<ScenarioStepRequest>) {
        steps.forEachIndexed { index, step ->
            if (index > 0) {
                `$`("$editorLocator [data-testid='scenario-root-add']")
                    .name("Кнопка добавления корневого шага.")
                    .click()
            }
            scenarioStep(index)
                .name("Блок шага ${step.number} detailed scenario.")
                .shouldBe(visible.because("блок шага ${step.number} должен быть видимым в модальном редакторе"))
            scenarioStepInput(index)
                .name("Поле текста шага ${step.number} detailed scenario.")
                .shouldBe(visible.because("поле текста шага ${step.number} должно быть видимым"))
                .typeOf(step.text)

            step.attachments.firstOrNull { it.content.isNotBlank() }?.let { attachment ->
                fillScenarioStepAttachment(index, attachment.name, attachment.content.trim())
            }
        }
    }

    @Step("Вводим Notes в модальном редакторе")
    fun fillNotes(notes: String) {
        notesTextarea.shouldBeVisibleForInput("Notes").typeOf(notes)
    }

    @Step("Сохраняем новый тест-кейс и проверяем закрытие модального редактора")
    fun saveNewTestCase() {
        saveButton.shouldBe(enabled.because("кнопка сохранения должна быть доступна после заполнения обязательных полей")).click()
        editor.shouldBe(disappear.because("после сохранения модальный редактор должен закрыться"))
    }

    @Step("Сохраняем изменения тест-кейса и проверяем закрытие модального редактора")
    fun saveChanges() {
        saveButton.shouldBe(enabled.because("кнопка сохранения изменений должна быть доступна")).click()
        editor.shouldBe(disappear.because("после сохранения изменений модальный редактор должен закрыться"))
    }

    @Step("В строке с Test ID {testId} выбираем Regress Run {status}")
    fun selectRegressionStatus(testId: String, status: String) {
        val rowLocator = savedRowLocator(testId)
        `$`(rowLocator).name("Строка тест-кейса $testId").shouldBe(visible)
        `$`("$rowLocator [data-testid='test-case-cell'][data-name='Regress Run'] [data-testid='regress-run-button']")
            .name("Select Regress Run в строке тест-кейса.")
            .shouldBe(enabled.because("select Regress Run должен быть доступен во время регресса"))
            .selectOption(status)
    }

    @Step("Открываем модальный редактор тест-кейса {testId} и устанавливаем Category / Feature {newValue}")
    fun updateCategory(testId: String, newValue: String) {
        openEditor(testId)
        categoryInput.shouldBeVisibleForInput("Category").setValue(newValue)
    }

    @Step("Прокручиваем к строке с Test ID {testId} и проверяем её видимость")
    fun checkRowVisible(testId: String) {
        `$`(savedRowLocator(testId)).name("Строка тест-кейса $testId")
            .scrollIntoView(CENTER)
            .shouldBe(visible.because("строка тест-кейса должна отображаться в таблице"))
    }

    @Step("Проверяем исчезновение строки с Test ID {testId}")
    fun checkRowDisappeared(testId: String) {
        `$`(savedRowLocator(testId)).name("Строка тест-кейса $testId")
            .shouldBe(disappear.because("строка тест-кейса должна исчезнуть"))
    }

    @Step("Проверяем количество сохранённых строк таблицы: {expectedCount}")
    fun checkSavedRowsCount(expectedCount: Int) {
        savedRows.shouldHave(size(expectedCount).because("количество строк должно соответствовать ожидаемому"))
    }

    @Step("Нажимаем Add Row и проверяем появление модального редактора создания")
    fun openCreateEditor() {
        addRowButton.shouldBe(enabled).click()
        editor.shouldBe(visible.because("после Add Row должен открыться модальный редактор"))
        testIdInput.shouldBe(enabled.because("в режиме создания Test ID должен быть доступен"))
    }

    @Step("Проверяем статус изменений модального редактора: {expectedStatus}")
    fun checkDirtyStatus(expectedStatus: String) {
        dirtyStatus.shouldHave(text(expectedStatus).because("статус должен отражать наличие несохранённых изменений"))
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
        editor.shouldBe(disappear.because("модальный редактор без изменений должен закрываться без предупреждения"))
    }

    @Step("Проверяем предупреждение о несохранённых изменениях")
    fun checkUnsavedChangesWarning() {
        editor.shouldBe(visible.because("модальный редактор с несохранёнными изменениями должен оставаться открытым"))
        unsavedChangesDialog
            .shouldBe(visible.because("при попытке закрытия должно появиться предупреждение"))
            .shouldHave(text("Сохранить изменения?").because("предупреждение должно предлагать сохранить изменения"))
            .shouldHave(text("У вас есть несохранённые изменения.").because("предупреждение должно объяснять причину показа"))
    }

    @Step("Отказываемся от сохранения изменений и проверяем закрытие модального редактора")
    fun discardUnsavedChanges() {
        discardUnsavedChangesButton.shouldBe(visible).click()
        editor.shouldBe(disappear.because("после отказа от сохранения модальный редактор должен закрыться"))
    }

    @Step("Продолжаем редактирование и проверяем возвращение в модальный редактор")
    fun continueEditing() {
        continueEditingButton.shouldBe(visible).click()
        unsavedChangesDialog.shouldBe(disappear.because("после продолжения редактирования предупреждение должно закрыться"))
        editor.shouldBe(visible.because("после закрытия предупреждения модальный редактор должен остаться открытым"))
    }

    @Step("Проверяем значение Category / Feature: {expectedCategory}")
    fun checkCategoryValue(expectedCategory: String) {
        categoryInput.shouldHave(value(expectedCategory).because("поле Category / Feature должно оставаться доступным для редактирования"))
    }

    @Step("Проверяем Ready Date в модальном редакторе: {expectedDate}")
    fun checkEditorReadyDate(expectedDate: String) {
        readyDateInput.shouldHave(value(expectedDate).because("Ready Date должна содержать ожидаемую дату"))
    }

    @Step("Проверяем Ready Date {expectedDate} в строке с Test ID {testId}")
    fun checkReadyDate(testId: String, expectedDate: String) {
        `$`("${savedRowLocator(testId)} [data-testid='test-case-cell'][data-name='Ready Date']")
            .name("Ячейка Ready Date в строке тест-кейса.")
            .shouldHave(text(expectedDate).because("Ready Date должна содержать ожидаемую дату"))
    }

    @Step("Открываем редактор тест-кейса {testId} и фокусируем Category / Feature")
    fun focusOnCategory(testId: String) {
        openEditor(testId)
        categoryInput.shouldBe(visible).click()
    }

    private fun openEditor(testId: String) {
        `$`("${savedRowLocator(testId)} [data-testid='scenario-edit']")
            .name("Кнопка изменения тест-кейса $testId.")
            .shouldBe(visible.because("кнопка изменения должна быть видимой в строке"))
            .click()
        editor.shouldBe(visible.because("после нажатия Изменить должен открыться модальный редактор"))
        testIdInput.shouldBe(disabled.because("в режиме изменения Test ID не должен редактироваться"))
    }

    @Step("Раскрываем шаг {stepIndex}, добавляем вложение и вводим его содержимое")
    private fun fillScenarioStepAttachment(stepIndex: Int, attachmentName: String, attachmentContent: String) {
        val step = scenarioStep(stepIndex)
        step.`$`("[data-testid='scenario-step-toggle'], .scenario-step-toggle .scenario-chevron")
            .name("Кнопка раскрытия шага.")
            .click()
        step.`$`("[data-testid='scenario-attachment-add-button']").name("Кнопка добавления вложения.").shouldBe(visible).click()
        val attachmentEditor = step.`$`("[data-testid='scenario-editor-attachment']").name("Редактор вложения шага.")
        attachmentEditor.`$`(".scenario-attachment-heading")
            .name("Поле имени вложения.")
            .shouldBe(visible.because("имя вложения должно редактироваться в заголовке"))
            .typeOf(attachmentName)
            .shouldHave(value(attachmentName).because("имя вложения должно сохраняться до общего сохранения"))
        attachmentEditor.`$`("[data-testid='scenario-attachment-toggle'], .scenario-attachment-summary .scenario-chevron")
            .name("Кнопка раскрытия вложения.")
            .click()
        attachmentEditor.`$`("[data-testid='scenario-attachment-content']")
            .name("Поле содержимого вложения.")
            .shouldBe(visible.because("поле содержимого должно быть видимым после раскрытия вложения"))
            .typeOf(attachmentContent)
            .shouldHave(value(attachmentContent).because("содержимое вложения должно сохраняться до общего сохранения"))
    }

    private fun scenarioStep(index: Int): SelenideElement =
        `$`("$editorLocator [data-testid='scenario-editor-step'][data-scenario-path='$index']")

    private fun scenarioStepInput(index: Int): SelenideElement =
        scenarioStep(index).`$`("[data-testid='scenario-step-input']")

    private fun modalFieldByLabel(label: String, element: String): SelenideElement =
        `$x`("//*[@data-testid='test-case-editor-modal']//label[.//span[normalize-space()='$label']]//$element")

    private fun savedRowLocator(testId: String): String =
        "$tableLocator [data-testid='test-case-row'][data-test-case-id='$testId']"
}
