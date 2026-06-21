package org.golenev.utils

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
                    ScenarioStepRequest(number = 1, text = "Определяем дату запуска теста; удаляем отчеты; создаём и обновляем запись через API", attachments = emptyList()),
                ),
            )
        ),
        ScenarioTemplate(
            category = "E2E_FOR_AUTOTEST",
            shortTitle = "Batch creates 10 items and report shows them",
            scenario = ScenarioRequest(
                steps = listOf(
                    ScenarioStepRequest(number = 1, text = "Создаём batch из 10 тестов и проверяем отображение в отчете", attachments = emptyList()),
                ),
            )
        ),
        ScenarioTemplate(
            category = "E2E_FOR_AUTOTEST",
            shortTitle = "Update existing test keeps readyDate",
            scenario = ScenarioRequest(
                steps = listOf(
                    ScenarioStepRequest(number = 1, text = "Обновляем существующий тест и проверяем сохранение readyDate", attachments = emptyList()),
                ),
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
