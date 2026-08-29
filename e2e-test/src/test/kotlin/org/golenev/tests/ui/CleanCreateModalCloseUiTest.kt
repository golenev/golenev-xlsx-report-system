package org.golenev.tests.ui

import com.codeborne.selenide.Selenide
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.ModalCloseAction
import org.golenev.ui.pages.mainPage
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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

    @ParameterizedTest(name = "Модальное окно без изменений закрывается {0}")
    @EnumSource(ModalCloseAction::class)
    @DisplayName("Модальное окно без изменений закрывается без предупреждения")
    fun shouldCloseCleanCreateModalWithoutWarning(action: ModalCloseAction) {
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
