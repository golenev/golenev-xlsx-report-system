package org.golenev.tests.ui

import com.codeborne.selenide.Selenide
import io.qameta.allure.AllureId
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.mainPage
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Проверяет защиту изменений при закрытии модального окна создания тест-кейса.
 */
@DisplayName("UI: Предупреждение при закрытии модального окна создания с изменениями")
class DirtyCreateModalCloseWarningUiTest {

    private val testId = "UI-DIRTY-MODAL"
    private val category = "Редактирование после предупреждения"

    @BeforeEach
    fun setUp() {
        step("Настраиваем драйвер Selenide") {
            DriverConfig().setup()
        }
    }

    @AfterEach
    fun tearDown() {
        step("Закрываем веб-драйвер") {
            Selenide.closeWebDriver()
        }
    }

    @Test
    @AllureId("307")
    @DisplayName("Изменения отклоняются после предупреждения, вызванного клавишей Esc")
    fun shouldWarnWhenClosingDirtyCreateModalByEscape() {
        checkClosingWithWarningTemplate("клавишей Esc") {
            mainPage.testCaseTable.closeEditorByEscape()
        }
        step("Нажимаем Не сохранять") {
            mainPage.testCaseTable.discardUnsavedChanges()
        }
        step("Проверяем, что несохранённый тест-кейс не появился в таблице") {
            mainPage.testCaseTable.checkRowDisappeared(testId)
        }
    }

    @Test
    @AllureId("308")
    @DisplayName("Изменения отклоняются после предупреждения, вызванного крестиком")
    fun shouldWarnWhenClosingDirtyCreateModalByCloseButton() {
        checkClosingWithWarningTemplate("крестиком") {
            mainPage.testCaseTable.closeEditorByCloseButton()
        }
        step("Нажимаем Не сохранять") {
            mainPage.testCaseTable.discardUnsavedChanges()
        }
        step("Проверяем, что несохранённый тест-кейс не появился в таблице") {
            mainPage.testCaseTable.checkRowDisappeared(testId)
        }
    }

    @Test
    @AllureId("309")
    @DisplayName("Изменения отклоняются после предупреждения, вызванного нажатием вне модалки")
    fun shouldWarnWhenClosingDirtyCreateModalByBackdropClick() {
        checkClosingWithWarningTemplate("нажатием вне модального окна") {
            mainPage.testCaseTable.closeEditorByBackdropClick()
        }
        step("Нажимаем Не сохранять") {
            mainPage.testCaseTable.discardUnsavedChanges()
        }
        step("Проверяем, что несохранённый тест-кейс не появился в таблице") {
            mainPage.testCaseTable.checkRowDisappeared(testId)
        }
    }

    @Test
    @AllureId("310")
    @DisplayName("Редактирование продолжается после предупреждения, вызванного клавишей Esc")
    fun shouldContinueEditingAfterWarningByEscape() {
        checkClosingWithWarningTemplate("клавишей Esc") {
            mainPage.testCaseTable.closeEditorByEscape()
        }
        step("Нажимаем Продолжить редактирование") {
            mainPage.testCaseTable.continueEditing()
        }
        step("Продолжаем редактировать Category / Feature") {
            mainPage.testCaseTable.fillCategory(category)
        }
        step("Проверяем, что редактирование после предупреждения работает") {
            mainPage.testCaseTable.checkCategoryValue(category)
            mainPage.testCaseTable.checkDirtyStatus("Есть несохранённые изменения")
        }
    }

    @Test
    @AllureId("311")
    @DisplayName("Редактирование продолжается после предупреждения, вызванного крестиком")
    fun shouldContinueEditingAfterWarningByCloseButton() {
        checkClosingWithWarningTemplate("крестиком") {
            mainPage.testCaseTable.closeEditorByCloseButton()
        }
        step("Нажимаем Продолжить редактирование") {
            mainPage.testCaseTable.continueEditing()
        }
        step("Продолжаем редактировать Category / Feature") {
            mainPage.testCaseTable.fillCategory(category)
        }
        step("Проверяем, что редактирование после предупреждения работает") {
            mainPage.testCaseTable.checkCategoryValue(category)
            mainPage.testCaseTable.checkDirtyStatus("Есть несохранённые изменения")
        }
    }

    @Test
    @AllureId("312")
    @DisplayName("Редактирование продолжается после предупреждения, вызванного нажатием вне модалки")
    fun shouldContinueEditingAfterWarningByBackdropClick() {
        checkClosingWithWarningTemplate("нажатием вне модального окна") {
            mainPage.testCaseTable.closeEditorByBackdropClick()
        }
        step("Нажимаем Продолжить редактирование") {
            mainPage.testCaseTable.continueEditing()
        }
        step("Продолжаем редактировать Category / Feature") {
            mainPage.testCaseTable.fillCategory(category)
        }
        step("Проверяем, что редактирование после предупреждения работает") {
            mainPage.testCaseTable.checkCategoryValue(category)
            mainPage.testCaseTable.checkDirtyStatus("Есть несохранённые изменения")
        }
    }

    /**
     * Выполняет общий сценарий защиты изменений при закрытии модалки указанным способом.
     */
    private fun checkClosingWithWarningTemplate(actionDescription: String, closeAction: () -> Unit) {
        step("Открываем главную страницу") {
            mainPage.open()
        }
        step("Открываем модальное окно создания тест-кейса") {
            mainPage.testCaseTable.openCreateEditor()
        }
        step("Проверяем исходное отсутствие изменений") {
            mainPage.testCaseTable.checkDirtyStatus("Нет изменений")
        }
        step("Вносим изменение в Test ID") {
            mainPage.testCaseTable.fillTestId(testId)
        }
        step("Проверяем появление признака несохранённых изменений") {
            mainPage.testCaseTable.checkDirtyStatus("Есть несохранённые изменения")
        }
        step("Пытаемся закрыть модальное окно $actionDescription") {
            closeAction()
        }
        step("Проверяем предупреждение и сохранение модального окна открытым") {
            mainPage.testCaseTable.checkUnsavedChangesWarning()
        }

    }

}
