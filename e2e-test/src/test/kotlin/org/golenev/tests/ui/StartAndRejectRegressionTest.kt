package org.golenev.tests.ui

import com.codeborne.selenide.Selenide
import io.kotest.matchers.nulls.shouldNotBeNull
import io.qameta.allure.AllureId
import org.golenev.commondto.Priority
import org.golenev.db.tables.regression.RegressionDao
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.endpoints.*
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.mainPage
import org.golenev.utils.getRandomTestId
import org.golenev.utils.shouldBe
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Тесты отмены и остановки регресса. Валидации")
class StartAndRejectRegressionTest {

    private val createdTestId: String = "UI-LOCK-${getRandomTestId()}"
    private val createdReleaseName: String = "regress-zopa-${getRandomTestId()}"
    private val reportService = ReportServiceDao()

    @BeforeEach
    fun setUp() {
        DriverConfig().setup()
    }

    @AfterEach
    fun tearDown() {
        Selenide.closeWebDriver()

        step("Удаляем созданный тест-кейс") {
            TestReportDao.deleteByTestId(createdTestId)
        }

        step("Удаляем созданный запуск регрессионного тестирования") {
            RegressionDao.deleteByReleaseName(createdReleaseName)
        }
    }

    @Test
    @AllureId("202")
    @DisplayName("Запускаем и пытаемся завершить регресс через UI, получая ошибку отсутствия статуса прогона у тестов с проверкой записи в БД")
    fun startAndStopRegressionViaUi() {
        val regressionDate =
            step("Определяем дату запуска регрессионного тестирования") { LocalDate.now() }

        val batchRequest = step("Готовим данные для добавления тест-кейса") {
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
                        scenario = ScenarioRequest(
                            steps = listOf(
                                ScenarioStepRequest(
                                    number = 1,
                                    text = "Создаём запись через API и удаляем через UI",
                                    attachments = emptyList()
                                )
                            )
                        ),
                    ),
                ),
            )
        }

        step("Добавляем подготовленный тест-кейс") {
            reportService.sendBatch(batchRequest)
        }

        step("Открываем главную страницу") { mainPage.open() }

        step("Запускаем регрессионное тестирование") {
            mainPage.testCaseTable.regressionWidget.startRegression(createdReleaseName)
        }

        val regression = step("Проверяем сохранение начатого регрессионного тестирования") {
            RegressionDao.findByReleaseName(createdReleaseName)
                .shouldNotBeNull()
        }

        step("Убеждаемся в корректности полей регрессионного тестирования") {
            regression.status.shouldBe("RUNNING", "regression.status не совпало с ожидаемым")
            regression.regressionDate.shouldBe(regressionDate, "regression.regressionDate не совпало с ожидаемым")
            regression.payload?.tests.shouldBe(null, "regression.payload?.tests не совпало с ожидаемым")
            regression.payload?.status.shouldBe(null, "regression.payload?.status не совпало с ожидаемым")
        }

        step("Отменяем регрессионное тестирование") {
            mainPage.testCaseTable.regressionWidget.stopRegress()
        }

        step("Проверяем предупреждение о необходимости заполнить результаты тестирования") {
            mainPage.warningPopup.checkDefaultRegressionWarning()
        }

        step("Закрываем предупреждение") {
            mainPage.warningPopup.close()
        }

        step("Отменяем регрессионное тестирование") {
            mainPage.testCaseTable.regressionWidget.cancelRegression()
        }

        step("Проверяем, что отменённый запуск регрессионного тестирования больше не сохранён") {
            RegressionDao.findByReleaseName(createdReleaseName)
                .shouldBe(null, "RegressionDao.findByReleaseName(createdReleaseName) не совпало с ожидаемым")
        }
    }
}