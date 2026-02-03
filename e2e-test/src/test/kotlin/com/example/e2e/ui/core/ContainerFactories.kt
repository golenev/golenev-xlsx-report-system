package com.example.e2e.ui.core

import com.example.e2e.ui.components.Button
import com.example.e2e.ui.components.Input
import com.example.e2e.ui.components.Row
import org.openqa.selenium.By

/**
 * DSL-фабрика для создания базового контейнера по CSS-селектору.
 */
val IContainer.byCss: Getter<String, Container>
    get() = Getter { value ->
        Container(by = By.cssSelector(value), context = this).withTitle(value)
    }

/**
 * DSL-фабрика для создания кнопки по CSS-селектору.
 */
val IContainer.button: Getter<String, Button>
    get() = Getter { value ->
        Button(context = this, by = By.cssSelector(value)).withTitle(value)
    }

/**
 * DSL-фабрика для создания инпута по CSS-селектору.
 */
val IContainer.input: Getter<String, Input>
    get() = Getter { value ->
        Input(context = this, by = By.cssSelector(value)).withTitle(value)
    }

/**
 * DSL-фабрика для поиска поля по data-test-id в текущем контексте.
 */
val IContainer.fieldByTestId: Getter<String, Container>
    get() = Getter { value ->
        Container(by = By.cssSelector("[data-test-id='${value}']"), context = this).withTitle(value)
    }

/**
 * DSL-фабрика для поиска поля по data-test-id внутри строки добавления новой записи.
 */
val IContainer.newRowFieldByTestId: Getter<String, Container>
    get() = Getter { value ->
        Container(by = By.cssSelector(".new-row [data-test-id='${value}']"), context = this).withTitle(value)
    }

/**
 * DSL-фабрика для строки таблицы по тестовому идентификатору.
 */
val IContainer.rowByTestId: Getter<String, Row>
    get() = Getter { value ->
        Row(by = By.cssSelector("tbody tr[data-test-id='tr-data-test-id-${value}']"), context = this)
            .withTitle("Row $value")
    }
