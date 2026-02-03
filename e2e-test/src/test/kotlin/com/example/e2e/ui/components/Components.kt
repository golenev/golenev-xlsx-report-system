package com.example.e2e.ui.components

import com.codeborne.selenide.SelenideElement
import com.example.e2e.ui.core.Container
import com.example.e2e.ui.core.IContainer
import com.example.e2e.utils.typeOf
import org.openqa.selenium.By

/**
 * Компонент-кнопка, наследует базовое поведение контейнера.
 */
open class Button : Container {
    constructor(self: SelenideElement) : super(self)
    constructor(context: IContainer? = null, by: By) : super(by, context)
    constructor(context: IContainer? = null, css: String) : super(By.cssSelector(css), context)
}

/**
 * Компонент текстового поля ввода с вводом через `typeOf`.
 */
open class Input : Container {
    constructor(self: SelenideElement) : super(self)
    constructor(context: IContainer? = null, by: By) : super(by, context)
    constructor(context: IContainer? = null, css: String) : super(By.cssSelector(css), context)

    /**
     * Вводит значение с использованием `typeOf`.
     */
    fun type(value: String) {
        self.typeOf(value)
    }
}

/**
 * Компонент многострочного ввода с вводом через `typeOf`.
 */
open class TextArea : Container {
    constructor(self: SelenideElement) : super(self)
    constructor(context: IContainer? = null, by: By) : super(by, context)
    constructor(context: IContainer? = null, css: String) : super(By.cssSelector(css), context)

    /**
     * Вводит значение с использованием `typeOf`.
     */
    fun type(value: String) {
        self.typeOf(value)
    }
}

/**
 * Компонент выпадающего списка/селекта.
 */
open class DropDown : Container {
    constructor(self: SelenideElement) : super(self)
    constructor(context: IContainer? = null, by: By) : super(by, context)
    constructor(context: IContainer? = null, css: String) : super(By.cssSelector(css), context)
}

/**
 * Компонент строки таблицы.
 */
open class Row : Container {
    constructor(self: SelenideElement) : super(self)
    constructor(context: IContainer? = null, by: By) : super(by, context)
    constructor(context: IContainer? = null, css: String) : super(By.cssSelector(css), context)
}
