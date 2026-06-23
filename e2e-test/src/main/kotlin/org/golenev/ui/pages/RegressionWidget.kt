package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.`$$`
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.SelenideElement
import org.golenev.utils.typeOf

class RegressionWidget {
    private val regressionActions = `$$`("div.regression-actions")
    private val regressionStartButton: SelenideElement get() = regressionActions.findBy(text("Would you run regress"))
    private val regressionReleaseInput: SelenideElement = element("input.release-input")
    private val regressionSaveButton: SelenideElement = element("div.regression-start-form button.success-btn")
    private val regressionCancelButton: SelenideElement = `$`(".regression-actions .secondary-btn")
    private val regressionStopButton: SelenideElement = `$`(".regression-actions .danger-btn")

    fun openStartForm() {
        regressionStartButton.shouldBe(enabled.because("элемент должен быть доступен перед кликом")).click()
        regressionReleaseInput.shouldBe(visible.because("элемент должен быть видимым на странице"))
    }

    fun fillReleaseName(releaseName: String) {
        regressionReleaseInput.shouldBe(visible.because("элемент должен быть видимым для ввода значения")).typeOf(releaseName)
    }

    fun saveRegressionStart() {
        regressionSaveButton.shouldBe(enabled.because("элемент должен быть доступен перед кликом")).click()
        regressionCancelButton.shouldBe(visible.because("элемент должен быть видимым на странице"))
    }

    fun startRegression(releaseName: String) {
        openStartForm()
        fillReleaseName(releaseName)
        saveRegressionStart()
    }

    fun cancelRegression() {
        regressionCancelButton.shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
        regressionCancelButton.should(disappear.because("элемент должен исчезнуть после выполненного действия"))
    }

    fun stopRegress() {
        regressionStopButton.shouldBe(visible.because("элемент должен быть видимым перед кликом")).click()
    }
}
