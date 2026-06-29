package org.golenev.tests.ui

import com.codeborne.selenide.Selenide
import io.qameta.allure.AllureId
import org.golenev.db.tables.testReportTable.TestReportDao
import org.golenev.restapi.endpoints.ReportServiceDao
import org.golenev.restapi.endpoints.TestBatchRequest
import org.golenev.ui.config.DriverConfig
import org.golenev.ui.pages.Application.mainPage
import org.golenev.ui.pages.Application.testCaseTable
import org.golenev.utils.TestDataGenerator
import org.golenev.utils.getRandomTestId
import org.golenev.utils.step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDate

@Isolated
@DisplayName("UI: Обновление таблицы после удаления тест-кейсов через API")
class ApiDeletionRefreshUiTest {

    private val reportService = ReportServiceDao()
    @BeforeEach
    fun setUp() {
        step("Полностью очищаем таблицу тест-кейсов перед тестом") {
            TestReportDao.truncate()
        }

        step("Настраиваем драйвер Selenide") {
            DriverConfig().setup()
        }
    }

    @AfterEach
    fun tearDown() {
        step("Закрываем веб-драйвер") {
            Selenide.closeWebDriver()
        }

        step("Очищаем таблицу тест-кейсов после теста") {
            TestReportDao.truncate()
        }
    }

    @Test
    @AllureId("303")
    @DisplayName("После API-удаления и refresh UI показывает актуальное количество строк")
    fun shouldShowActualRowsAfterApiDeletionAndRefresh() {
        val readyDate = step("Фиксируем текущую дату для генерации тест-кейсов") {
            LocalDate.now().toString()
        }
        val testCases = step("Генерируем данные для трёх тест-кейсов") {
            TestDataGenerator.generateTestCases(count = 3, readyDate = readyDate)
                .mapIndexed { index, testCase ->
                    val testId = "UI-API-DEL-${getRandomTestId()}-${index + 1}"
                    testCase.copy(
                        testId = testId,
                        issueLink = "https://youtrack.test/issue/$testId",
                    )
                }
        }
        step("Создаём три тест-кейса через API") {
            reportService.sendForceBatch(TestBatchRequest(items = testCases))
        }

        step("Открываем главную страницу") {
            mainPage.open()
        }

        step("Проверяем, что в таблице отображаются ровно три строки") {
            testCaseTable.checkSavedRowsCount(3)
            testCases.forEach { testCase -> testCaseTable.checkRowVisible(testCase.testId.orEmpty()) }
        }

        val deletedTestId = testCases[1].testId.orEmpty()
        val remainingTestIds = testCases.mapNotNull { it.testId }.filterNot { it == deletedTestId }

        step("Удаляем через API один выбранный тест-кейс: $deletedTestId") {
            reportService.deleteTest(deletedTestId)
        }

        step("Обновляем страницу и проверяем, что удалился именно $deletedTestId, а в таблице осталось две строки") {
            mainPage.refreshCurrentPage()
            testCaseTable.checkSavedRowsCount(2)
            testCaseTable.checkRowDisappeared(deletedTestId)
            remainingTestIds.forEach { testId -> testCaseTable.checkRowVisible(testId) }
        }

        step("Удаляем через API оставшиеся тест-кейсы") {
            remainingTestIds.forEach { testId -> reportService.deleteTest(testId) }
        }

        step("Снова обновляем страницу и проверяем, что таблица пустая") {
            mainPage.refreshCurrentPage()
            testCaseTable.checkSavedRowsCount(0)
            remainingTestIds.forEach { testId -> testCaseTable.checkRowDisappeared(testId) }
        }
    }
}
