package org.golenev.ui.pages

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.CollectionCondition.sizeGreaterThan
import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.*
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
    private val editorLocator = "[data-testid='test-case-editor-modal']"

    private val addRowButton =
        `$`("[data-action='add-row']")
            .name("Кнопка Add Row, которая открывает модальный редактор нового тест-кейса.")

    private val editor =
        `$`(editorLocator).name("Модальный редактор создания или изменения тест-кейса.")

    private val testIdInput = `$`("[data-testid='test-id-input']").name("Поле Test ID в модальном редакторе.")
    private val categoryInput = `$`("[data-testid='category-input']").name("Поле Category / Feature в модальном редакторе.")
    private val shortTitleInput = `$`("[data-testid='short-title-input']").name("Поле Short Title в модальном редакторе.")
    private val issueLinkInput = `$`("[data-testid='youtrack-link']").name("Поле YouTrack Issue Link в модальном редакторе.")
    private val generalStatusSelect = `$`("$editorLocator [data-testid='status-dropdown']").name("Select General Test Status в модальном редакторе.")
    private val prioritySelect = `$`("$editorLocator [data-testid='priority-select']").name("Select Priority в модальном редакторе.")
    private val notesTextarea = `$`("[data-testid='notes-input']").name("Поле Notes в модальном редакторе.")
    private val saveButton = `$`("$editorLocator [data-testid='save-test-case-button']").name("Кнопка сохранения модального редактора.")
    private val readyDateInput = `$`("[data-testid='ready-date-input']").name("Поле Ready Date в модальном редакторе.")
    private val dirtyStatus = `$`(".test-case-modal-dirty").name("Статус несохранённых изменений модального редактора.")
    private val closeButton = `$`(".test-case-modal-close").name("Крестик закрытия модального редактора.")
    private val backdrop = `$`(".test-case-modal-backdrop").name("Область вне модального окна.")
    private val unsavedChangesDialog = `$`(".unsaved-confirm").name("Предупреждение о несохранённых изменениях.")
    private val discardUnsavedChangesButton = `$`(".unsaved-discard").name("Кнопка Не сохранять в предупреждении.")
    private val continueEditingButton = `$`(".unsaved-confirm .secondary-btn").name("Кнопка Продолжить редактирование в предупреждении.")

    private val savedRows =
        `$$`("[data-testid='test-case-row']").name("Сохранённые строки тест-кейсов")

    private val activeFilterChips =
        `$$`("[data-testid='active-filter-chip']").name("Чипы активных фильтров таблицы")

    private val clearAllFiltersButton =
        `$`("[data-action='clear-all-filters']").name("Кнопка сброса всех активных фильтров.")

    private val groupingSelect =
        `$`("[data-testid='table-group-select']").name("Select группировки строк таблицы.")

    private val emptyFilterResult =
        `$`("[data-testid='table-empty-result']").name("Сообщение об отсутствии строк по заданным фильтрам.")

    private val scenarioRootAddButton = `$`("[data-testid='scenario-root-add']")
        .name("Кнопка добавления корневого шага.")
    private val scenarioSteps = `$$`("[data-testid='scenario-editor-step']")
        .name("Шаги detailed scenario в модальном редакторе.")
    private val filterButtons = `$$`("[data-testid='table-filter-button']")
        .name("Кнопки фильтров колонок таблицы.")
    private val filterSearchInputs = `$$`("[data-testid='table-filter-search']")
        .name("Поля поиска фильтров колонок.")
    private val filterOptions = `$$`("[data-testid='table-filter-option']")
        .name("Значения фильтров колонок.")
    private val applyFilterButtons = `$$`("[data-action='apply-filter']")
        .name("Кнопки применения фильтров колонок.")
    private val filterPanels = `$$`("[data-testid='table-filter-panel']")
        .name("Панели фильтров колонок таблицы.")
    private val groupRows = `$$`("[data-testid='table-group-row']")
        .name("Группы строк таблицы.")

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
        val row = savedRows.findBy(attribute("data-test-case-id", testId)).name("Строка тест-кейса $testId")
        row.shouldBe(visible)
        row.`$`("[data-testid='regress-run-button']")
            .name("Select Regress Run в строке тест-кейса.")
            .shouldBe(enabled.because("select Regress Run должен быть доступен во время регресса"))
            .selectOption(status)
    }

    @Step("Устанавливаем Category / Feature {newValue} в открытом модальном редакторе")
    fun updateCategory(newValue: String) {
        categoryInput.shouldBeVisibleForInput("Category").setValue(newValue)
    }

    @Step("Прокручиваем к строке с Test ID {testId} и проверяем её видимость")
    fun checkRowVisible(testId: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).name("Строка тест-кейса $testId")
            .scrollIntoView(CENTER)
            .shouldBe(visible.because("строка тест-кейса должна отображаться в таблице"))
    }

    @Step("Проверяем исчезновение строки с Test ID {testId}")
    fun checkRowDisappeared(testId: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).name("Строка тест-кейса $testId")
            .shouldBe(disappear.because("строка тест-кейса должна исчезнуть"))
    }

    @Step("Проверяем количество сохранённых строк таблицы: {expectedCount}")
    fun checkSavedRowsCount(expectedCount: Int) {
        savedRows.shouldHave(size(expectedCount).because("количество строк должно соответствовать ожидаемому"))
    }

    @Step("Проверяем наличие фильтра в каждой колонке таблицы: {columnKeys}")
    fun checkColumnFilterButtons(columnKeys: List<String>) {
        columnKeys.forEach { columnKey ->
            filterButtons.findBy(attribute("data-name", columnKey)).name("Кнопка фильтра колонки $columnKey.")
                .shouldBe(visible.because("кнопка фильтра должна отображаться в колонке $columnKey"))
                .shouldHave(attribute("data-state", "inactive"))
        }
    }

    @Step("Открываем фильтр колонки {columnKey}")
    fun openColumnFilter(columnKey: String) {
        filterButtons.findBy(attribute("data-name", columnKey)).name("Кнопка фильтра колонки $columnKey.").shouldBe(enabled).click()
        filterPanels.findBy(attribute("data-name", columnKey))
            .name("Панель фильтра колонки $columnKey.").shouldBe(visible.because("панель фильтра колонки $columnKey должна открыться"))
    }

    @Step("Задаём текстовый фильтр колонки {columnKey}: {query}")
    fun setTextFilter(columnKey: String, query: String) {
        filterSearchInputs.findBy(attribute("data-name", columnKey))
            .name("Поле текстового фильтра колонки $columnKey.")
            .shouldBe(visible)
            .typeOf(query)
    }

    @Step("Выбираем значения фильтра колонки {columnKey}: {values}")
    fun selectFilterValues(columnKey: String, values: List<String>) {
        values.forEach { value ->
            filterOptions.filterBy(attribute("data-name", columnKey))
                .findBy(attribute("data-value", value)).`$`("input")
                .name("Значение $value фильтра колонки $columnKey.")
                .shouldBe(enabled.because("значение фильтра должно быть доступно для выбора"))
                .click()
        }
    }

    @Step("Применяем фильтр колонки {columnKey}")
    fun applyColumnFilter(columnKey: String) {
        applyFilterButtons.findBy(attribute("data-name", columnKey))
            .name("Кнопка применения фильтра колонки $columnKey.")
            .shouldBe(enabled)
            .click()
        filterPanels.findBy(attribute("data-name", columnKey))
            .name("Панель фильтра колонки $columnKey.").shouldBe(disappear.because("после применения панель фильтра должна закрыться"))
        filterButtons.findBy(attribute("data-name", columnKey))
            .name("Кнопка фильтра колонки $columnKey.").shouldHave(attribute("data-state", "active"))
    }

    @Step("Закрываем фильтр колонки {columnKey} без применения")
    fun closeColumnFilterWithoutApplying(columnKey: String) {
        actions().sendKeys(Keys.ESCAPE).perform()
        filterPanels.findBy(attribute("data-name", columnKey))
            .name("Панель фильтра колонки $columnKey.").shouldBe(disappear.because("панель фильтра должна закрыться без применения черновика"))
    }

    @Step("Проверяем активный фильтр колонки {columnKey} с описанием {expectedDescription}")
    fun checkActiveFilter(columnKey: String, expectedDescription: String) {
        `$`("[data-testid='active-filter-chip'][data-name='$columnKey']")
            .name("Чип активного фильтра колонки $columnKey.")
            .shouldBe(visible)
            .shouldHave(text(expectedDescription))
    }

    @Step("Проверяем количество активных фильтров: {expectedCount}")
    fun checkActiveFiltersCount(expectedCount: Int) {
        activeFilterChips.shouldHave(size(expectedCount).because("количество чипов должно совпадать с количеством активных фильтров"))
    }

    @Step("Удаляем активный фильтр колонки {columnKey}")
    fun removeActiveFilter(columnKey: String) {
        `$`("[data-testid='active-filter-chip'][data-name='$columnKey']")
            .name("Чип активного фильтра колонки $columnKey.")
            .shouldBe(enabled)
            .click()
        filterButtons.findBy(attribute("data-name", columnKey))
            .name("Кнопка фильтра колонки $columnKey.").shouldHave(attribute("data-state", "inactive"))
    }

    @Step("Сбрасываем все активные фильтры")
    fun clearAllFilters() {
        clearAllFiltersButton.shouldBe(enabled).click()
        activeFilterChips.shouldHave(size(0).because("после общего сброса активных фильтров не должно остаться"))
    }

    @Step("Проверяем сообщение об отсутствии тест-кейсов по заданным фильтрам")
    fun checkEmptyFilterResult() {
        emptyFilterResult
            .shouldBe(visible.because("для пустого результата должно отображаться отдельное сообщение"))
            .shouldHave(text("По заданным фильтрам тест-кейсы не найдены"))
    }

    @Step("Группируем строки таблицы по колонке {columnKey}")
    fun groupBy(columnKey: String) {
        groupingSelect.shouldBe(visible).selectOptionByValue(columnKey)
        groupingSelect.shouldHave(value(columnKey))
    }

    @Step("Проверяем группу {groupValue} для колонки {columnKey}")
    fun checkGroupVisible(columnKey: String, groupValue: String) {
        groupRows.filterBy(attribute("data-name", columnKey)).findBy(attribute("data-value", groupValue))
            .name("Группа $groupValue колонки $columnKey.")
            .shouldBe(visible.because("группа $groupValue должна отображаться после группировки"))
            .shouldHave(attribute("data-state", "expanded"))
    }

    @Step("Проверяем отсутствие группы {groupValue} для колонки {columnKey}")
    fun checkGroupDisappeared(columnKey: String, groupValue: String) {
        groupRows.filterBy(attribute("data-name", columnKey)).findBy(attribute("data-value", groupValue))
            .name("Группа $groupValue колонки $columnKey.")
            .shouldBe(disappear.because("группа $groupValue не должна отображаться"))
    }

    @Step("Сворачиваем группу {groupValue} колонки {columnKey}")
    fun collapseGroup(columnKey: String, groupValue: String) {
        groupRows.filterBy(attribute("data-name", columnKey)).findBy(attribute("data-value", groupValue))
            .name("Группа $groupValue колонки $columnKey.").`$`("[data-testid='table-group-toggle']")
            .name("Кнопка сворачивания группы $groupValue.")
            .shouldBe(enabled)
            .click()
        groupRows.filterBy(attribute("data-name", columnKey)).findBy(attribute("data-value", groupValue))
            .name("Группа $groupValue колонки $columnKey.").shouldHave(attribute("data-state", "collapsed"))
    }

    @Step("Проверяем, что группировка выбрана по колонке {columnKey}")
    fun checkGrouping(columnKey: String) {
        groupingSelect.shouldHave(value(columnKey))
        groupRows
            .shouldHave(sizeGreaterThan(0).because("после выбора группировки должны отображаться заголовки групп"))
    }

    @Step("Проверяем, что группировка не выбрана")
    fun checkGroupingInactive() {
        groupingSelect.shouldHave(exactValue(""))
        groupRows
            .shouldHave(size(0).because("без группировки заголовки групп не должны отображаться"))
    }

    @Step("Проверяем доступность редактирования Regress Run для тест-кейса {testId}")
    fun checkRegressionStatusEditable(testId: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).`$`("[data-testid='regress-run-button']")
            .name("Select Regress Run в строке тест-кейса $testId.")
            .shouldBe(enabled.because("фильтрация не должна блокировать редактирование Regress Run"))
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
        savedRows.findBy(attribute("data-test-case-id", testId)).`$`("[data-name='Ready Date']")
            .name("Ячейка Ready Date в строке тест-кейса.")
            .shouldHave(text(expectedDate).because("Ready Date должна содержать ожидаемую дату"))
    }

    @Step("Открываем модальный редактор тест-кейса {testId}")
    fun openEditor(testId: String) {
        savedRows.findBy(attribute("data-test-case-id", testId)).`$`("[data-testid='scenario-edit']")
            .name("Кнопка изменения тест-кейса $testId.")
            .shouldBe(visible.because("кнопка изменения должна быть видимой в строке"))
            .click()
        editor.shouldBe(visible.because("после нажатия Изменить должен открыться модальный редактор"))
        testIdInput.shouldBe(disabled.because("в режиме изменения Test ID не должен редактироваться"))
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
