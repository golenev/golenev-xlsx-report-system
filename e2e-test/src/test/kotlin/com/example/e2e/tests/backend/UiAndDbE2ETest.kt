package com.example.e2e.tests.backend

import com.codeborne.selenide.Selenide
import com.example.e2e.db.repository.RegressionRepository
import com.example.e2e.db.repository.TestReportRepository
import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.Priority
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.ui.config.DriverConfig
import com.example.e2e.ui.pages.MainPage
import com.example.e2e.utils.step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("API + UI + DB: Связанные проверки отчёта и регресса")
class UiAndDbE2ETest {

    private val reportService = ReportService()
    private val mainPage = MainPage()
    private lateinit var createdTestId: String
    private lateinit var createdReleaseName: String

    @BeforeEach
    fun setUp() {
        DriverConfig().setup()
    }

    @AfterEach
    fun tearDown() {
        step("Закрываем веб-драйвер") {
            Selenide.closeWebDriver()
        }

        if (this::createdTestId.isInitialized) {
            step("Удаляем созданный тест-кейс из базы") {
                TestReportRepository.deleteByTestId(createdTestId)
            }
        }

        if (this::createdReleaseName.isInitialized) {
            step("Удаляем созданный регресс из базы") {
                RegressionRepository.deleteByReleaseName(createdReleaseName)
            }
        }
    }

    @Test
    @AllureId("200")
    @DisplayName("Создаём кейс через API, удаляем через UI и проверяем отсутствие в БД")
    fun createCaseViaApiAndDeleteViaUi() {
        val readyDate = step("Фиксируем текущую дату") { LocalDate.now().minusDays(Random.nextLong(1, 30)) }
        createdTestId = step("Генерируем идентификатор теста") { "API-UI-${UUID.randomUUID().toString().take(8)}" }

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

    @Test
    @AllureId("201")
    @DisplayName("Запускаем и отменяем регресс через UI с проверкой записи в БД")
    fun startAndCancelRegressionViaUi() {
        val regressionDate = step("Определяем дату запуска регресса") { LocalDate.now().minusDays(Random.nextLong(31, 60)) }
        createdReleaseName = step("Готовим имя регресса") { "regress-$regressionDate-${UUID.randomUUID().toString().take(6)}" }

        step("Удаляем из базы потенциальные конфликты по имени регресса") {
            RegressionRepository.deleteByReleaseName(createdReleaseName)
        }

        step("Открываем главную страницу") { mainPage.open() }

        step("Запускаем регресс через UI") {
            mainPage.startRegression(createdReleaseName)
        }

        val regression = step("Проверяем создание записи о регрессе в базе") {
            RegressionRepository.findByReleaseName(createdReleaseName)
                .shouldNotBeNull()
        }

        step("Убеждаемся в корректности полей регресса") {
            regression.status shouldBe "RUNNING"
            regression.regressionDate shouldBe regressionDate
            regression.payload?.trim() shouldBe "{}"
        }

        step("Отменяем регресс через UI") {
            mainPage.cancelRegression()
        }

        step("Проверяем, что запись о регрессе удалена") {
            RegressionRepository.findByReleaseName(createdReleaseName) shouldBe null
        }
    }
}
