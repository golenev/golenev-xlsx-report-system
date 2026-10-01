package org.golenev.tests.ui.filters

import com.codeborne.selenide.Selenide
import org.golenev.restapi.endpoints.*
import org.golenev.ui.config.DriverConfig
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

abstract class FilterUiTestBase {
    protected val reportService = ReportServiceDao()
    private val createdTestIds = mutableListOf<String>()

    @BeforeEach
    fun setUpFilterUiTest() {
        step("Готовим приложение к работе") {
            DriverConfig().setup()
        }
    }

    @AfterEach
    fun tearDownFilterUiTest() {
        step("Завершаем работу с приложением") {
            Selenide.closeWebDriver()
        }
        step("Удаляем созданные для теста предварительные данные") {
            createdTestIds.forEach { testId -> reportService.deleteTest(testId) }
            createdTestIds.clear()
        }
    }

    protected fun createPreliminaryTestDataViaApi(fixture: FilterFixture) {
        reportService.sendForceBatch(TestBatchRequest(items = fixture.items))
        createdTestIds += fixture.items.mapNotNull { it.testId }
    }

    protected fun filterFixture(): FilterFixture {
        val token = getRandomTestId().toString()
        val categoryAlpha = "FILTER_ALPHA_$token"
        val categoryBeta = "FILTER_BETA_$token"
        val firstId = "UI-FILTER-$token-1"
        val secondId = "UI-FILTER-$token-2"
        val thirdId = "UI-FILTER-$token-3"
        val first = testCase(
            testId = firstId,
            category = categoryAlpha,
            shortTitle = "Успешный вход $token",
            readyDate = "2026-09-01",
            generalStatus = "Готово",
            priority = "Critical",
            scenario = "Открываем форму авторизации $token",
            notes = "smoke-$token",
        )
        val second = testCase(
            testId = secondId,
            category = categoryAlpha,
            shortTitle = "Ошибка входа $token",
            readyDate = "2026-09-02",
            generalStatus = "Бэклог",
            priority = "Blocker",
            scenario = "Вводим неверный пароль $token",
            notes = null,
        )
        val third = testCase(
            testId = thirdId,
            category = categoryBeta,
            shortTitle = "Редактирование профиля $token",
            readyDate = "2026-09-02",
            generalStatus = "Готово",
            priority = "Medium",
            scenario = "Меняем имя пользователя $token",
            notes = "regression-$token",
        )
        return FilterFixture(
            items = listOf(first, second, third),
            firstId = firstId,
            secondId = secondId,
            thirdId = thirdId,
            categoryAlpha = categoryAlpha,
            categoryBeta = categoryBeta,
            token = token,
        )
    }

    protected fun encodeQueryValue(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun testCase(
        testId: String,
        category: String,
        shortTitle: String,
        readyDate: String,
        generalStatus: String,
        priority: String,
        scenario: String,
        notes: String?,
    ) = TestUpsertItem(
        testId = testId,
        category = category,
        shortTitle = shortTitle,
        issueLink = "https://youtrack.test/issue/$testId",
        readyDate = readyDate,
        generalStatus = generalStatus,
        priority = priority,
        scenario = ScenarioRequest(
            steps = listOf(
                ScenarioStepRequest(
                    number = 1,
                    text = scenario,
                    attachments = emptyList(),
                ),
            ),
        ),
        notes = notes,
    )
}

data class FilterFixture(
    val items: List<TestUpsertItem>,
    val firstId: String,
    val secondId: String,
    val thirdId: String,
    val categoryAlpha: String,
    val categoryBeta: String,
    val token: String,
)
