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
 * Проверяет закрытие модального окна создания тест-кейса, пока пользователь не изменил данные.
 */
@DisplayName("UI: Закрытие модального окна создания без изменений")
class CleanCreateModalCloseUiTest {

    @BeforeEach
    fun setUp() {
        DriverConfig().setup()
    }

    @AfterEach
    fun tearDown() {
        step("Завершаем работу с приложением") {
            Selenide.closeWebDriver()
        }
    }

    @Test
    @AllureId("304")
    @DisplayName("Модальное окно без изменений закрывается клавишей Esc")
    fun shouldCloseCleanCreateModalByEscape() {
        checkClosingWithoutWarningTemplate("клавишей Esc") {
            mainPage.testCaseEditor.closeEditorByEscape()
        }
    }

    @Test
    @AllureId("305")
    @DisplayName("Модальное окно без изменений закрывается крестиком")
    fun shouldCloseCleanCreateModalByCloseButton() {
        checkClosingWithoutWarningTemplate("крестиком") {
            mainPage.testCaseEditor.closeEditorByCloseButton()
        }
    }

    @Test
    @AllureId("306")
    @DisplayName("Модальное окно без изменений закрывается нажатием вне модального окна")
    fun shouldCloseCleanCreateModalByBackdropClick() {
        checkClosingWithoutWarningTemplate("нажатием вне модального окна") {
            mainPage.testCaseEditor.closeEditorByBackdropClick()
        }
    }

    /**
     * Выполняет общий сценарий закрытия неизменённой модалки указанным способом.
     */
    private fun checkClosingWithoutWarningTemplate(actionDescription: String, closeAction: () -> Unit) {
        step("Открываем главную страницу") {
            mainPage.open()
        }
        step("Проверяем заголовок открытой страницы") { mainPage.header.checkTitle() }
        step("Открываем модальное окно создания тест-кейса") {
            mainPage.header.openCreateEditor()
        }
        step("Проверяем открытие редактора создания") { mainPage.testCaseEditor.checkVisible() }
        step("Проверяем готовность режима создания") { mainPage.testCaseEditor.checkCreateModeReady() }
        step("Проверяем отсутствие изменений") {
            mainPage.testCaseEditor.footer.checkDirtyStatus("Нет изменений")
        }
        step("Закрываем модальное окно $actionDescription") {
            closeAction()
        }
        step("Проверяем, что модальное окно закрылось без предупреждения") {
            mainPage.testCaseEditor.checkEditorClosed()
        }
    }
}
