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
    /** Контейнеры действий regression widget, внутри которых находится кнопка старта регресса. */
    private val regressionActions = `$$`("div.regression-actions")
    /** Кнопка открытия формы запуска regression run. */
    private val regressionStartButton: SelenideElement get() = regressionActions.findBy(text("Would you run regress"))
    /** Поле ввода release name для нового regression run. */
    private val regressionReleaseInput: SelenideElement = element("input.release-input")
    /** Кнопка сохранения формы запуска regression run. */
    private val regressionSaveButton: SelenideElement = element("div.regression-start-form button.success-btn")
    /** Кнопка отмены текущего regression run. */
    private val regressionCancelButton: SelenideElement = `$`(".regression-actions .secondary-btn")
    /** Кнопка остановки текущего regression run. */
    private val regressionStopButton: SelenideElement = `$`(".regression-actions .danger-btn")

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
}
