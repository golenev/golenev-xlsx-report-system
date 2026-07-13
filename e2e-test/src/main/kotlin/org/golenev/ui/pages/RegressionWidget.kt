package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import org.golenev.ui.allure.name
import org.golenev.utils.typeOf
import io.qameta.allure.Step

/**
 * Component Object глобального виджета управления regression run в шапке страницы.
 */
class RegressionWidget {
    private val regressionStartButton: SelenideElement =
        `$$`("div.regression-actions")
            .findBy(text("Would you run regress"))
            .name("Кнопка открытия формы запуска regression run.")

    private val regressionReleaseInput: SelenideElement =
        element("input.release-input").name("Поле ввода release name для нового regression run.")

    private val regressionSaveButton: SelenideElement =
        element("div.regression-start-form button.success-btn").name("Кнопка сохранения формы запуска regression run.")

    private val regressionCancelButton: SelenideElement =
        `$`(".regression-actions .secondary-btn").name("Кнопка отмены текущего regression run.")

    private val regressionStopButton: SelenideElement =
        `$`(".regression-actions .danger-btn").name("Кнопка остановки текущего regression run.")

    @Step("Открываем форму запуска regression run и проверяем видимость поля release name")
    fun openStartForm() {
        regressionStartButton.shouldBe(enabled.because("кнопка открытия формы запуска regression run должна быть доступна перед кликом")).click()
        regressionReleaseInput.shouldBe(visible.because("поле ввода release name должно быть видимым после открытия формы запуска regression run"))
    }

    @Step("Заполняем release name в форме запуска regression run: {releaseName}")
    fun fillReleaseName(releaseName: String) {
        regressionReleaseInput.shouldBe(visible.because("поле ввода release name должно быть видимым для ввода значения")).typeOf(releaseName)
    }

    @Step("Сохраняем форму запуска regression run и проверяем, что появилась кнопка отмены")
    fun saveRegressionStart() {
        regressionSaveButton.shouldBe(enabled.because("кнопка сохранения формы запуска regression run должна быть доступна перед кликом")).click()
        regressionCancelButton.shouldBe(visible.because("кнопка отмены regression run должна быть видимой после запуска regression run"))
    }

    @Step("Полностью запускаем regression run: открываем форму, вводим release name {releaseName} и сохраняем")
    fun startRegression(releaseName: String) {
        openStartForm()
        fillReleaseName(releaseName)
        saveRegressionStart()
    }

    @Step("Отменяем активный regression run и проверяем исчезновение кнопки отмены")
    fun cancelRegression() {
        regressionCancelButton.shouldBe(visible.because("кнопка отмены regression run должна быть видимой перед кликом")).click()
        regressionCancelButton.should(disappear.because("кнопка отмены regression run должна исчезнуть после отмены regression run"))
    }

    @Step("Нажимаем кнопку остановки активного regression run")
    fun stopRegress() {
        regressionStopButton.shouldBe(visible.because("кнопка остановки regression run должна быть видимой перед кликом")).click()
    }
}
