package com.example.report.dto

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.NotEmpty

/**
 * Тело пакетного запроса на создание или обновление тест-кейсов.
 *
 * Аннотация `@field:NotEmpty` требует, чтобы список `items` был передан и не был пустым.
 *
 * @property items список тест-кейсов, которые нужно применить одним batch-upsert.
 */
data class TestBatchRequest(
    @field:NotEmpty
    val items: List<TestUpsertItem>
)

/**
 * DTO для создания или обновления одного тест-кейса.
 *
 * @property testId внешний идентификатор теста; нормализуется trim-ом и не меняется у существующей записи.
 * @property category категория или feature тест-кейса.
 * @property shortTitle краткое название тест-кейса.
 * @property issueLink ссылка на задачу; относится к ручным полям и обновляется только при `forceUpdate=true`.
 * @property readyDate дата готовности; при создании выставляется сервером, при обновлении принимается только с `forceUpdate=true`.
 * @property generalStatus общий статус теста из справочника.
 * @property priority приоритет теста из справочника.
 * @property scenario структурированный сценарий теста со шагами и вложениями.
 * @property notes произвольные заметки; относятся к ручным полям.
 * @property runStatus статус прогона во время активного регресса.
 */
data class TestUpsertItem(
    val testId: String? = null,
    val category: String? = null,
    val shortTitle: String? = null,
    val issueLink: String? = null,
    val readyDate: String? = null,
    val generalStatus: String? = null,
    val priority: String? = null,
    val scenario: ScenarioRequest? = null,
    val notes: String? = null,
    /**
     * Аннотация `@JsonAlias("run_status")` позволяет принимать поле как в Kotlin-формате `runStatus`,
     * так и в snake_case-формате `run_status`, который могут отправлять внешние клиенты.
     */
    @JsonAlias("run_status")
    val runStatus: String? = null,
)

/**
 * Структурированный сценарий тест-кейса.
 *
 * @property steps упорядоченный список шагов сценария; пустой список используется как безопасное значение по умолчанию.
 */
data class ScenarioRequest(
    val steps: List<ScenarioStepRequest> = emptyList(),
)

/**
 * Один шаг структурированного сценария.
 *
 * @property number номер шага, который сохраняет порядок действий в сценарии.
 * @property text текст действия или проверки на шаге.
 * @property attachments вложения шага, например request/response, извлечённые из Allure-отчёта.
 */
data class ScenarioStepRequest(
    val number: Int? = null,
    val text: String? = null,
    val attachments: List<ScenarioAttachmentRequest>? = null,
)

/**
 * Вложение шага сценария.
 *
 * @property type тип вложения, например `request`, `response` или другое человекочитаемое имя.
 * @property content содержимое вложения, которое показывается пользователю и экспортируется в отчёт.
 */
data class ScenarioAttachmentRequest(
    val type: String? = null,
    val content: String? = null,
)
