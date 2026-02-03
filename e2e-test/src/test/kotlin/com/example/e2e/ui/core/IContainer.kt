package com.example.e2e.ui.core

import com.codeborne.selenide.SelenideElement
import org.openqa.selenium.By

/**
 * Базовый контракт контейнера в Component Object Model.
 *
 * Контейнер знает свой локатор, контекст и позволяет централизовать базовые действия
 * (например, автоскролл и проверку видимости) для читаемых шагов теста.
 */
interface IContainer {
    /** Корневой элемент контейнера. */
    val self: SelenideElement

    /** Контекст, относительно которого выполнен поиск элемента. */
    var context: IContainer?

    /** Локатор, использованный для поиска элемента. */
    var by: By

    /** Человекочитаемое название элемента для логов/отчётов. */
    var title: String

    /** Включить/отключить автоскролл перед действием. */
    var autoScroll: Boolean

    /** Скроллит к элементу, если он не отображается. */
    fun scrollToIfNotVisible()
}
