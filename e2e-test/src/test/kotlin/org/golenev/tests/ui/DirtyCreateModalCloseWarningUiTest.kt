package org.golenev.tests.ui

import com.codeborne.selenide.Selenide
import io.qameta.allure.AllureId
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.ModalCloseAction
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
    @DisplayName("При закрытии изменённого модального окна клавишей Esc появляется предупреждение")
    fun shouldWarnWhenClosingDirtyCreateModalByEscape() {
        checkClosingWithWarning(ModalCloseAction.ESCAPE)
    }

    @Test
    @AllureId("308")
    @DisplayName("При закрытии изменённого модального окна крестиком появляется предупреждение")
    fun shouldWarnWhenClosingDirtyCreateModalByCloseButton() {
        checkClosingWithWarning(ModalCloseAction.CLOSE_BUTTON)
    }

    @Test
    @AllureId("309")
    @DisplayName("При закрытии изменённого модального окна нажатием вне модалки появляется предупреждение")
    fun shouldWarnWhenClosingDirtyCreateModalByBackdropClick() {
        checkClosingWithWarning(ModalCloseAction.BACKDROP)
    }

    /**
     * Выполняет общий сценарий защиты изменений при закрытии модалки указанным способом.
     */
    private fun checkClosingWithWarning(action: ModalCloseAction) {
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
            mainPage.testCaseTable.fillTestId("UI-DIRTY-MODAL")
        }
        step("Проверяем появление признака несохранённых изменений") {
            mainPage.testCaseTable.checkDirtyStatus("Есть несохранённые изменения")
        }
        step("Пытаемся закрыть модальное окно $action") {
            mainPage.testCaseTable.closeEditor(action)
        }
        step("Проверяем предупреждение и сохранение модального окна открытым") {
            mainPage.testCaseTable.checkUnsavedChangesWarning()
        }
    }
}
