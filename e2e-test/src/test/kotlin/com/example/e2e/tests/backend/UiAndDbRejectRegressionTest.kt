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
import com.example.e2e.utils.getRandomTestId
import com.example.e2e.utils.step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.qameta.allure.AllureId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("API + UI + DB: Тесты отмены и остановки регресса")
class UiAndDbRejectRegressionTest {

    private val mainPage = MainPage()
    private val createdTestId: String = "UI-LOCK-${getRandomTestId()}"
    private val createdReleaseName: String = "regress-zopa-${getRandomTestId()}"
    private val reportService = ReportService()

    @BeforeEach
    fun setUp() {
        DriverConfig().setup()
    }

    @AfterEach
    fun tearDown() {
        Selenide.closeWebDriver()

        step("Удаляем созданный тест-кейс из базы") {
            TestReportRepository.deleteByTestId(createdTestId)
        }

        step("Удаляем созданный регресс из базы") {
            RegressionRepository.deleteByReleaseName(createdReleaseName)
        }

    }

    @Test
    @AllureId("201")
    @DisplayName("Запускаем и отменяем регресс через UI с проверкой записи в БД")
    fun startAndCancelRegressionViaUi() {
        val regressionDate = step("Определяем дату запуска регресса") { LocalDate.now() }

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
            regression.payload?.tests shouldBe null
            regression.payload?.status shouldBe null
        }

        step("Отменяем регресс через UI") {
            mainPage.cancelRegression()
        }

        step("Проверяем, что запись о регрессе удалена из БД") {
            RegressionRepository.findByReleaseName(createdReleaseName) shouldBe null
        }
    }

    @Test
    @AllureId("202")
    @DisplayName("Запускаем и пытаемся завершить регресс через UI, получая ошибку отсутствия статуса прогона у тестов с проверкой записи в БД")
    fun startAndStopRegressionViaUi() {
        val regressionDate = step("Определяем дату запуска регресса") { LocalDate.now() }

        val batchRequest = step("Готовим batch-запрос для создания теста") {
            TestBatchRequest(
                items = listOf(
                    TestUpsertItem(
                        testId = createdTestId,
                        category = "API+UI",
                        shortTitle = "Создан через API",
                        issueLink = "https://youtrack.test/issue/$createdTestId",
                        readyDate = regressionDate.toString(),
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
            regression.payload?.tests shouldBe null
            regression.payload?.status shouldBe null
        }

        step("Отменяем регресс через UI") {
            mainPage.stopRegress()
        }

        step("Убеждаемся, что появился popup warning с предупреждением и необходимости заполнения результатов прогона") {
            mainPage.checkPopupWarning()
        }

        step("Закрываем popup warning") {
            mainPage.closePopupWarning()
        }

        step("Отменяем регресс через UI") {
            mainPage.cancelRegression()
        }

        step("Проверяем, что запись о регрессе удалена из БД") {
            RegressionRepository.findByReleaseName(createdReleaseName) shouldBe null
        }
    }
}
