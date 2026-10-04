package org.golenev.ui.pages

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.element
import org.golenev.ui.allure.name
import org.golenev.utils.typeOf

/**
 * Component Object глобального виджета управления regression run в заголовке таблицы.
 *
 * Последовательно открываем форму regression run, вводим release name {releaseName} и нажимаем сохранение
 *
 * Этот маршрут тест выполняет явно: открытие формы, проверка поля, ввод имени,
 * сохранение и проверка запущенного регресса представлены отдельными вызовами.
 */
class RegressionWidget {
    private val regressionStartButton =
        `$`("[data-testid='regression-start-button']")
            .name("Кнопка открытия формы запуска regression run.")

    private val regressionReleaseInput =
        element(".release-input").name("Поле ввода release name для нового regression run.")

    private val regressionSaveButton =
        element(".regression-start-form .success-btn").name("Кнопка сохранения формы запуска regression run.")

    private val regressionCancelButton =
        `$`(".regression-actions .secondary-btn").name("Кнопка отмены текущего regression run.")

    private val regressionStopButton =
        `$`(".regression-actions .danger-btn").name("Кнопка остановки текущего regression run.")

    /**
     * Дожидаемся доступности кнопки запуска regression run, нажимаем её и проверяем видимость поля release name
     */
    fun openStartForm() {
        regressionStartButton.shouldBe(enabled.because("кнопка открытия формы запуска regression run должна быть доступна перед кликом")).click()
    }

    /**
     * Дожидаемся видимости поля release name и вводим значение {releaseName}
     */
    fun fillReleaseName(releaseName: String) {
        regressionReleaseInput.shouldBe(visible.because("поле ввода release name должно быть видимым для ввода значения")).typeOf(releaseName)
    }

    /**
     * Дожидаемся доступности кнопки сохранения regression run, нажимаем её и проверяем видимость кнопки отмены
     */
    fun saveRegressionStart() {
        regressionSaveButton.shouldBe(enabled.because("кнопка сохранения формы запуска regression run должна быть доступна перед кликом")).click()
    }

    /**
     * Дожидаемся видимости кнопки отмены regression run, нажимаем её и проверяем исчезновение кнопки
     */
    fun cancelRegression() {
        regressionCancelButton.shouldBe(visible.because("кнопка отмены regression run должна быть видимой перед кликом")).click()
        regressionCancelButton.should(disappear.because("кнопка отмены regression run должна исчезнуть после отмены regression run"))
    }

    /**
     * Дожидаемся видимости кнопки Stop regression run и нажимаем её
     */
    fun stopRegress() {
        regressionStopButton.shouldBe(visible.because("кнопка остановки regression run должна быть видимой перед кликом")).click()
    }

    /**
     * Проверяем завершение regression run
     */
    fun checkRegressionStopped() {
        regressionStartButton.shouldBe(visible.because("кнопка запуска regression run должна появиться после завершения regression run"))
    }

    /**
     * Проверяем видимость поля имени релиза
     */
    fun checkReleaseNameInputVisible() {
        regressionReleaseInput.shouldBe(visible.because("поле ввода release name должно быть видимым после открытия формы запуска regression run"))
    }

    /**
     * Проверяем доступность отмены запущенного регресса
     */
    fun checkRegressionStarted() {
        regressionCancelButton.shouldBe(visible.because("кнопка отмены regression run должна быть видимой после запуска regression run"))
    }
}
