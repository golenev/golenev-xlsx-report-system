package com.example.e2e.ui.core

import com.codeborne.selenide.ScrollIntoViewOptions
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.SelenideElement
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException

/**
 * Делает XPath относительным для корректного поиска внутри контекста.
 *
 * Если передан абсолютный XPath (не начинается с "." или "("),
 * добавляет ведущую точку, чтобы поиск выполнялся от `context.self`.
 */
fun safeXpath(by: By): By {
    return if (by is By.ByXPath) {
        val raw = by.toString().removePrefix("By.xpath: ").trim()
        if (raw.isNotEmpty() && !raw.startsWith(".") && !raw.startsWith("(")) {
            By.xpath(".$raw")
        } else {
            by
        }
    } else {
        by
    }
}

/**
 * Базовая реализация контейнера с единым поведением клика, скролла и ввода.
 *
 * Объединяет `IContainer` и делегирует интерфейс `SelenideElement`,
 * чтобы сохранять доступ к стандартным selenide-операциям при необходимости.
 */
open class Container(override val self: SelenideElement) : IContainer, SelenideElement by self {
    override var context: IContainer? = null
    override var by: By = By.xpath(".")
    override var title: String = ""
    override var autoScroll: Boolean = true

    /**
     * Создаёт контейнер по локатору и контексту.
     *
     * При наличии контекста поиск выполняется относительно `context.self`.
     */
    constructor(by: By, context: IContainer?) : this(
        self = context?.self?.find(safeXpath(by)) ?: Selenide.`$`(by)
    ) {
        this.by = by
        this.context = context
    }

    /**
     * Скроллит к элементу, если он вне видимости.
     */
    override fun scrollToIfNotVisible() {
        if (!autoScroll) return
        if (!self.isDisplayed) {
            self.scrollIntoView(ScrollIntoViewOptions.instant().block(ScrollIntoViewOptions.Block.center))
        }
    }

    /**
     * Кликает по элементу с автоскроллом и ретраями при stale reference.
     */
    fun click() {
        scrollToIfNotVisible()
        var lastError: StaleElementReferenceException? = null
        repeat(3) {
            try {
                self.click()
                return
            } catch (error: StaleElementReferenceException) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("Failed to click container")
    }

    /**
     * Устанавливает значение в поле с автоскроллом.
     */
    fun setValue(value: String) {
        scrollToIfNotVisible()
        self.setValue(value)
    }

    /**
     * Отправляет нажатия клавиш с автоскроллом.
     */
    fun sendKeys(vararg keys: CharSequence) {
        scrollToIfNotVisible()
        self.sendKeys(*keys)
    }
}

/**
 * Назначает человекочитаемый title для контейнера и возвращает его.
 */
fun <T : IContainer> T.withTitle(value: String): T = apply { title = value }
