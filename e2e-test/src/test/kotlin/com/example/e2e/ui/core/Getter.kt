package com.example.e2e.ui.core

/**
 * Унифицированный геттер для DSL-фабрик компонентной модели.
 *
 * Используется для создания компонентов по значению (CSS, data-test-id и т.п.),
 * чтобы запись выглядела как `button["..."]` или `rowByTestId["..."]`.
 */
fun interface Getter<V, R> {
    /**
     * Создаёт и возвращает объект по заданному значению.
     */
    operator fun get(value: V): R
}
