package org.golenev.tests.ui

import com.codeborne.selenide.Selenide
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.golenev.db.repository.TestReportRepository
import org.golenev.dto.GeneralTestStatus
import org.golenev.dto.Priority
import org.golenev.dto.TestBatchRequest
import org.golenev.dto.TestUpsertItem
import org.golenev.service.ReportService
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.MainPage
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.random.Random

@DisplayName("Удаление тест кейса через UI")
class DeleteTestCaseByUI {

    private val mainPage = MainPage()
    private val createdTestId: String = "UI-LOCK-${getRandomTestId()}"
    private val reportService = ReportService()

    @BeforeEach
    fun setUp() {
        DriverConfig().setup()
    }

    @AfterEach
    fun tearDown() {
        step("Закрываем веб-драйвер") {
            Selenide.closeWebDriver()
        }

        step("Удаляем созданный тест-кейс из базы") {
            TestReportRepository.deleteByTestId(createdTestId)
        }

    }

    @Test
    @AllureId("200")
    @DisplayName("Создаём кейс через API, удаляем через UI и проверяем отсутствие в БД")
    fun createCaseViaApiAndDeleteViaUi() {
        val readyDate = step("Фиксируем текущую дату") { LocalDate.now().minusDays(Random.Default.nextLong(1, 30)) }

        val batchRequest = step("Готовим batch-запрос для создания теста") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = createdTestId,
                        category = "API+UI",
                        shortTitle = "Создан через API",
                        issueLink = "https://youtrack.test/issue/$createdTestId",
                        readyDate = readyDate.toString(),
                        generalStatus = GeneralTestStatus.QUEUE.value,
                        priority = Priority.MEDIUM.value,
                        scenario = "Создаём запись через API и удаляем через UI",
                    ),
                ),
            )
        }

        step("Создаём запись через API") {
            reportService.sendBatch(batchRequest)
        }

        step("Открываем страницу отчёта") {
            mainPage.open()
        }

        step("Убеждаемся, что созданный кейс отображается") {
            mainPage.shouldSeeTestCase(createdTestId)
        }

        step("Удаляем кейс через интерфейс") {
            mainPage.deleteTestCase(createdTestId)
            mainPage.shouldNotSeeTestCase(createdTestId)
        }

        val remainingItems = step("Проверяем отсутствие записи в базе данных") {
            TestReportRepository.countByTestId(createdTestId)
        }

        step("Подтверждаем, что записи нет") {
            remainingItems shouldBe 0
        }
    }

}