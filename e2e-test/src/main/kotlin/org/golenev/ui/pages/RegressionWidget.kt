package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import org.golenev.utils.typeOf

/**
 * Component Object глобального виджета управления regression run в шапке страницы.
 */
class RegressionWidget {
    private val regressionActions =
        `$$`(REGRESSION_ACTIONS_LOCATOR).`as`("Контейнеры действий regression widget, внутри которых находится кнопка старта регресса.")
    private val regressionStartButton: SelenideElement get() =
        regressionActions.findBy(text("Would you run regress")).asReportElement("Кнопка открытия формы запуска regression run.", "$REGRESSION_ACTIONS_LOCATOR >> text(Would you run regress)")
    private val regressionReleaseInput: SelenideElement =
        element(RELEASE_INPUT_LOCATOR).asReportElement("Поле ввода release name для нового regression run.", RELEASE_INPUT_LOCATOR)
    private val regressionSaveButton: SelenideElement =
        element(REGRESSION_SAVE_BUTTON_LOCATOR).asReportElement("Кнопка сохранения формы запуска regression run.", REGRESSION_SAVE_BUTTON_LOCATOR)
    private val regressionCancelButton: SelenideElement =
        `$`(REGRESSION_CANCEL_BUTTON_LOCATOR).asReportElement("Кнопка отмены текущего regression run.", REGRESSION_CANCEL_BUTTON_LOCATOR)
    private val regressionStopButton: SelenideElement =
        `$`(REGRESSION_STOP_BUTTON_LOCATOR).asReportElement("Кнопка остановки текущего regression run.", REGRESSION_STOP_BUTTON_LOCATOR)

    /** Открывает форму запуска regression run и проверяет видимость поля release name. */
    fun openStartForm() {
        regressionStartButton.shouldBe(enabled.because("кнопка открытия формы запуска regression run должна быть доступна перед кликом")).click()
        regressionReleaseInput.shouldBe(visible.because("поле ввода release name должно быть видимым после открытия формы запуска regression run"))
    }

    /** Заполняет release name в форме запуска regression run. */
    fun fillReleaseName(releaseName: String) {
        regressionReleaseInput.shouldBe(visible.because("поле ввода release name должно быть видимым для ввода значения")).typeOf(releaseName)
    }

    /** Сохраняет форму запуска regression run и проверяет, что появилась кнопка отмены. */
    fun saveRegressionStart() {
        regressionSaveButton.shouldBe(enabled.because("кнопка сохранения формы запуска regression run должна быть доступна перед кликом")).click()
        regressionCancelButton.shouldBe(visible.because("кнопка отмены regression run должна быть видимой после запуска regression run"))
    }

    /** Полностью запускает regression run: открывает форму, вводит release name и сохраняет. */
    fun startRegression(releaseName: String) {
        openStartForm()
        fillReleaseName(releaseName)
        saveRegressionStart()
    }

    /** Отменяет активный regression run и проверяет исчезновение кнопки отмены. */
    fun cancelRegression() {
        regressionCancelButton.shouldBe(visible.because("кнопка отмены regression run должна быть видимой перед кликом")).click()
        regressionCancelButton.should(disappear.because("кнопка отмены regression run должна исчезнуть после отмены regression run"))
    }

    /** Нажимает кнопку остановки активного regression run. */
    fun stopRegress() {
        regressionStopButton.shouldBe(visible.because("кнопка остановки regression run должна быть видимой перед кликом")).click()
    }
    private companion object {
        private const val REGRESSION_ACTIONS_LOCATOR = "div.regression-actions"
        private const val RELEASE_INPUT_LOCATOR = "input.release-input"
        private const val REGRESSION_SAVE_BUTTON_LOCATOR = "div.regression-start-form button.success-btn"
        private const val REGRESSION_CANCEL_BUTTON_LOCATOR = ".regression-actions .secondary-btn"
        private const val REGRESSION_STOP_BUTTON_LOCATOR = ".regression-actions .danger-btn"
    }
}
