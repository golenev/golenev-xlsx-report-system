package com.example.e2e.utils

import com.example.e2e.dto.TestUpsertItem
import java.time.LocalDate


object TestDataGenerator {

    /**
     * Шаблон данных для TestUpsertItem (кроме testId и readyDate)
     */
    private data class ScenarioTemplate(
        val category: String,
        val shortTitle: String,
        val issueLink: String? = "https://youtrackru/issue/",
        val generalStatus: String = "Готово",
        val priority: String = "Medium",
        val scenario: String
    )

    /**
     * Несколько реалистичных сценариев (по мотивам твоего JSON).
     * Важно: используем raw string, чтобы не страдать с \n и экранированием.
     */
    private val templates: List<ScenarioTemplate> = listOf(
        ScenarioTemplate(
            category = "E2E_FOR_AUTOTEST",
            shortTitle = "Ready date auto set",
            scenario =
                """
                **Сценарий**:
                • 1. Определяем дату запуска теста
                • 2. Удаляем отчеты за выбранную дату
                • 3. Формируем batch-запрос без readyDate
                • 4. Отправляем batch на создание записи
                  • 4.1. POST запрос к /api/tests/batch
                    ```
                    Attachment: Request (...-attachment.html)
                      POST to http://localhost:18080/api/tests/batch
                      Body
                      {
                        "items": [
                          {
                            "testId": "<GENERATED>",
                            "category": "E2E_FOR_AUTOTEST",
                            "shortTitle": "Ready date auto set",
                            "readyDate": "<READY_DATE>",
                            "generalStatus": "Готово",
                            "priority": "Medium",
                            "scenario": "Сценарий. шаг 1 шаг 2 шаг 3"
                          }
                        ]
                      }
                    ```
                • 5. Получаем созданную запись и проверяем readyDate
                • 6. Готовая дата установлена на сегодняшнее число
                • 7. Формируем batch-запрос с попыткой изменить readyDate для уже имеющейся записи
                • 8. Отправляем batch на обновление записи
                • 9. Получаем обновленную запись
                • 10. Готовая дата осталась прежней, остальные поля обновлены
                """.trimIndent()
        ),
        ScenarioTemplate(
            category = "E2E_FOR_AUTOTEST",
            shortTitle = "Batch creates 10 items and report shows them",
            scenario =
                """
                **Сценарий**:
                • 1. Определяем дату запуска теста
                • 2. Удаляем отчеты за выбранную дату
                • 3. Формируем batch-запрос с 10 тестами (readyDate задан явно)
                • 4. Отправляем batch на обновление тестов (POST /api/tests/batch)
                • 5. Запрашиваем отчет о тестах (GET /api/tests)
                • 6. Фильтруем записи по readyDate
                • 7. Проверяем, что количество записей за дату равно количеству отправленных items
                • 8. Для каждого testId проверяем: category, shortTitle, readyDate, status, priority, updatedAt
                """.trimIndent()
        ),
        ScenarioTemplate(
            category = "E2E_FOR_AUTOTEST",
            shortTitle = "Update existing test keeps readyDate",
            scenario =
                """
                **Сценарий**:
                • 1. Создаем запись через batch (readyDate = <READY_DATE>)
                • 2. Повторно отправляем batch с тем же testId, но с другим readyDate (readyDate = <READY_DATE_MINUS_1>)
                • 3. Получаем запись через GET /api/tests
                • 4. Проверяем: readyDate не изменился, остальные поля обновились (scenario/priority/category и т.д.)
                """.trimIndent()
        )
    )

    fun generateTestCases(count: Int = 20, readyDate: String): List<TestUpsertItem> {
        return (1..count).map { index ->
            val template = templates[(index - 1) % templates.size]

            TestUpsertItem(
                testId = generateTestId(index),
                category = template.category,
                shortTitle = template.shortTitle,
                issueLink = template.issueLink,
                readyDate = readyDate,
                generalStatus = template.generalStatus,
                priority = template.priority,
                scenario = template.scenario
                    .replace("<READY_DATE>", readyDate)
                    .replace("<READY_DATE_MINUS_1>", LocalDate.parse(readyDate).minusDays(1).toString())
                    .replace("<GENERATED>", generateTestId(index)),
                notes = ""
            )
        }
    }

    private fun generateTestId(index: Int): String =
        "TC-%05d-%d".format(index, index)
}
