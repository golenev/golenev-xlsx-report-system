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
 * Проверяет закрытие модального окна создания тест-кейса, пока пользователь не изменил данные.
 */
@DisplayName("UI: Закрытие модального окна создания без изменений")
class CleanCreateModalCloseUiTest {

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
    @AllureId("304")
    @DisplayName("Модальное окно без изменений закрывается клавишей Esc")
    fun shouldCloseCleanCreateModalByEscape() {
        checkClosingWithoutWarningTemplate(ModalCloseAction.ESCAPE)
    }

    @Test
    @AllureId("305")
    @DisplayName("Модальное окно без изменений закрывается крестиком")
    fun shouldCloseCleanCreateModalByCloseButton() {
        checkClosingWithoutWarningTemplate(ModalCloseAction.CLOSE_BUTTON)
    }

    @Test
    @AllureId("306")
    @DisplayName("Модальное окно без изменений закрывается нажатием вне модального окна")
    fun shouldCloseCleanCreateModalByBackdropClick() {
        checkClosingWithoutWarningTemplate(ModalCloseAction.BACKDROP)
    }

    /**
     * Выполняет общий сценарий закрытия неизменённой модалки указанным способом.
     */
    private fun checkClosingWithoutWarningTemplate(action: ModalCloseAction) {
        step("Открываем главную страницу") {
            mainPage.open()
        }
        step("Открываем модальное окно создания тест-кейса") {
            mainPage.testCaseTable.openCreateEditor()
        }
        step("Проверяем отсутствие изменений") {
            mainPage.testCaseTable.checkDirtyStatus("Нет изменений")
        }
        step("Закрываем модальное окно $action") {
            mainPage.testCaseTable.closeEditor(action)
        }
        step("Проверяем, что модальное окно закрылось без предупреждения") {
            mainPage.testCaseTable.checkEditorClosed()
        }
    }
}
