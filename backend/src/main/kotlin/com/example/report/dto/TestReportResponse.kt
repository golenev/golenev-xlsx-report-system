package com.example.report.dto

import java.time.LocalDate

/**
 * Ответ API с текущим отчётом тест-кейсов и настройками отображения таблицы.
 *
 * @property items строки отчёта по тест-кейсам.
 * @property columnConfig ширины колонок, загруженные из конфигурации.
 * @property translations человекочитаемые подписи или переводы ключей для UI.
 */
data class TestReportResponse(
    val items: List<TestReportItemDto>,
    val columnConfig: Map<String, Int>,
    val translations: Map<String, String> = emptyMap()
)

/**
 * Строка тест-кейса в ответе отчёта.
 *
 * @property testId внешний идентификатор теста.
 * @property category категория или feature теста.
 * @property shortTitle краткое название.
 * @property issueLink ссылка на задачу.
 * @property readyDate дата готовности тест-кейса.
 * @property generalStatus общий статус готовности или автоматизации.
 * @property priority приоритет.
 * @property scenario структурированный сценарий со шагами и вложениями.
 * @property notes заметки пользователя.
 * @property updatedAt время последнего обновления строкой для отображения в UI.
 * @property runStatus статус текущего регрессионного прогона, если регресс активен.
 */
data class TestReportItemDto(
    val testId: String,
    val category: String?,
    val shortTitle: String?,
    val issueLink: String?,
    val readyDate: LocalDate?,
    val generalStatus: String?,
    val priority: String?,
    val scenario: ScenarioRequest?,
    val notes: String?,
    val updatedAt: String?,
    val runStatus: String?,
)
