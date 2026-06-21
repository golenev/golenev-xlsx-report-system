package org.golenev.utils

import org.golenev.restapi.endpoints.ScenarioAttachmentRequest
import org.golenev.restapi.endpoints.ScenarioRequest
import org.golenev.restapi.endpoints.ScenarioStepRequest
import org.golenev.restapi.endpoints.TestUpsertItem


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
        val scenario: ScenarioRequest
    )

    /**
     * Несколько реалистичных сценариев в structured-формате scenario.steps.
     */
    private val templates: List<ScenarioTemplate> = listOf(
        ScenarioTemplate(
            category = "E2E_FOR_AUTOTEST",
            shortTitle = "Ready date auto set",
            scenario = ScenarioRequest(
                steps = listOf(
                    ScenarioStepRequest(
                        number = 1,
                        text = "Очищаем ранее сформированные отчёты по тестовому набору",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 2,
                        text = "Создаём новую запись тест-кейса через API без передачи readyDate",
                        attachments = listOf(
                            ScenarioAttachmentRequest(
                                type = "text",
                                content = """
                                POST /api/tests?forceUpdate=true
                                Проверяется, что readyDate не передаётся в теле запроса и должен быть установлен автоматически.
                            """.trimIndent()
                            )
                        )
                    ),
                    ScenarioStepRequest(
                        number = 3,
                        text = "Проверяем, что сервер сохранил тест-кейс с автоматически рассчитанной датой готовности",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 4,
                        text = "Запрашиваем отчёт и находим созданный тест-кейс по testId",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 5,
                        text = "Сравниваем readyDate в ответе API и в строке отчёта",
                        attachments = listOf(
                            ScenarioAttachmentRequest(
                                type = "text",
                                content = """
                                Ожидаемый результат:
                                readyDate в отчёте совпадает с датой, рассчитанной backend при создании тест-кейса.
                            """.trimIndent()
                            )
                        )
                    )
                )
            )
        ),
        ScenarioTemplate(
            category = "E2E_FOR_AUTOTEST",
            shortTitle = "Batch creates 10 items and report shows them",
            scenario = ScenarioRequest(
                steps = listOf(
                    ScenarioStepRequest(
                        number = 1,
                        text = "Формируем batch из десяти тест-кейсов с уникальными testId",
                        attachments = listOf(
                            ScenarioAttachmentRequest(
                                type = "text",
                                content = """
                                Batch содержит 10 элементов.
                                Каждый элемент должен иметь уникальный testId, одинаковую category и общий readyDate.
                            """.trimIndent()
                            )
                        )
                    ),
                    ScenarioStepRequest(
                        number = 2,
                        text = "Отправляем batch-запрос на создание тест-кейсов через API",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 3,
                        text = "Проверяем успешный статус ответа и отсутствие ошибок валидации",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 4,
                        text = "Открываем отчёт и фильтруем строки по категории E2E_FOR_AUTOTEST",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 5,
                        text = "Проверяем, что все десять созданных тест-кейсов отображаются в отчёте",
                        attachments = listOf(
                            ScenarioAttachmentRequest(
                                type = "text",
                                content = """
                                Проверка выполняется по списку testId из исходного batch-запроса.
                                Каждый testId должен присутствовать в отчёте ровно один раз.
                            """.trimIndent()
                            )
                        )
                    ),
                    ScenarioStepRequest(
                        number = 6,
                        text = "Проверяем, что значения shortTitle, priority и generalStatus соответствуют отправленным данным",
                        attachments = emptyList()
                    )
                )
            )
        ),
        ScenarioTemplate(
            category = "E2E_FOR_AUTOTEST",
            shortTitle = "Update existing test keeps readyDate",
            scenario = ScenarioRequest(
                steps = listOf(
                    ScenarioStepRequest(
                        number = 1,
                        text = "Создаём исходный тест-кейс с заполненными category, shortTitle, priority и scenario",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 2,
                        text = "Получаем сохранённую запись и фиксируем первоначальное значение readyDate",
                        attachments = listOf(
                            ScenarioAttachmentRequest(
                                type = "text",
                                content = """
                                readyDate используется как контрольное значение.
                                При последующем обновлении оно не должно быть перезаписано.
                            """.trimIndent()
                            )
                        )
                    ),
                    ScenarioStepRequest(
                        number = 3,
                        text = "Отправляем повторный запрос с тем же testId и изменёнными shortTitle, priority и notes",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 4,
                        text = "Проверяем, что существующая запись была обновлена, а не создана повторно",
                        attachments = emptyList()
                    ),
                    ScenarioStepRequest(
                        number = 5,
                        text = "Проверяем, что readyDate остался равен первоначальному значению",
                        attachments = listOf(
                            ScenarioAttachmentRequest(
                                type = "text",
                                content = """
                                Ожидаемый результат:
                                readyDate после update совпадает с readyDate после create.
                            """.trimIndent()
                            )
                        )
                    ),
                    ScenarioStepRequest(
                        number = 6,
                        text = "Проверяем, что обновлённые поля отображаются в отчёте без дублирования строки",
                        attachments = emptyList()
                    )
                )
            )
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
                scenario = template.scenario,
                notes = ""
            )
        }
    }

    private fun generateTestId(index: Int): String =
        "TC-%05d-%d".format(index, index)
}
