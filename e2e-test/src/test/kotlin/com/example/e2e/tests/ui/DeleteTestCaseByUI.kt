package com.example.e2e.tests.ui

import com.codeborne.selenide.Selenide
import com.example.e2e.db.repository.TestReportRepository
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.Priority
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.getRandomTestId
import com.example.e2e.utils.step
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
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