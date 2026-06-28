package com.example.report.service

import com.example.report.config.ColumnConfigProperties
import com.example.report.dto.ScenarioAttachmentRequest
import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import com.example.report.dto.TestReportItemDto
import com.example.report.dto.TestReportResponse
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.time.LocalDate

class ExcelExportServiceUnitTest {

    private val testReportService: TestReportService = Mockito.mock(TestReportService::class.java)
    private val columnConfigService: ColumnConfigService = Mockito.mock(ColumnConfigService::class.java)
    private val service = ExcelExportService(testReportService, columnConfigService)

    /**
     * Позитивный unit-тест проверяет генерацию XLSX из текущего отчёта: заголовки, ширины колонок и форматирование
     * structured scenario с вложениями должны быть читаемы в workbook.
     */
    @Test
    fun `generate workbook renders headers widths and formatted structured scenario`() {
        Mockito.`when`(testReportService.getReport()).thenReturn(
            TestReportResponse(
                items = listOf(
                    TestReportItemDto(
                        testId = "T-1",
                        category = "API",
                        shortTitle = "Exports scenario",
                        issueLink = "https://issue/T-1",
                        readyDate = LocalDate.parse("2026-06-28"),
                        generalStatus = "Готово",
                        priority = "High",
                        scenario = ScenarioRequest(
                            listOf(
                                ScenarioStepRequest(1, " открыть отчёт ", listOf(ScenarioAttachmentRequest("text", "payload"))),
                                ScenarioStepRequest(2, "   ", emptyList()),
                            ),
                        ),
                        notes = "note",
                        updatedAt = null,
                        runStatus = null,
                    ),
                ),
                columnConfig = mapOf("testId" to 100, "scenario" to 200),
            ),
        )

        val workbook = XSSFWorkbook(ByteArrayInputStream(service.generateWorkbook()))
        workbook.use {
            val sheet = it.getSheet("Test Report")
            assertCellEquals("Test ID", sheet.getRow(0).getCell(0).stringCellValue, "A1")
            assertCellEquals("Detailed Scenario", sheet.getRow(0).getCell(7).stringCellValue, "H1")
            assertEquals(100 * 40, sheet.getColumnWidth(0), "Ширина testId должна применяться из columnConfig")
            assertEquals(200 * 40, sheet.getColumnWidth(7), "Ширина scenario должна применяться из columnConfig")
            assertCellEquals("T-1", sheet.getRow(1).getCell(0).stringCellValue, "A2")
            assertCellEquals("1. открыть отчёт\n   [text] payload", sheet.getRow(1).getCell(7).stringCellValue, "H2")
        }
    }

    /**
     * Edge-тест проверяет генерацию XLSX из regression snapshot: невалидные элементы tests отбрасываются,
     * Map-сценарий форматируется, а строковый scenario сохраняется как есть.
     */
    @Test
    fun `generate workbook from snapshot filters invalid rows and formats map scenario`() {
        Mockito.`when`(columnConfigService.getConfig()).thenReturn(ColumnConfigProperties(mapOf("testId" to 90)))

        val bytes = service.generateWorkbookFromSnapshot(
            mapOf(
                "tests" to listOf(
                    mapOf(
                        "testId" to "S-1",
                        "category" to "API",
                        "scenario" to mapOf("steps" to listOf(mapOf("number" to 1, "text" to "step", "attachments" to listOf(mapOf("type" to "log", "content" to "trace"))))),
                    ),
                    mapOf("category" to "NO-ID", "scenario" to "must be skipped"),
                    "not-a-map",
                    mapOf("testId" to "S-2", "scenario" to "plain scenario"),
                ),
            ),
        )

        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        workbook.use {
            val sheet = it.getSheet("Test Report")
            assertEquals(2, sheet.lastRowNum, "В workbook должны попасть только две валидные строки snapshot")
            assertCellEquals("S-1", sheet.getRow(1).getCell(0).stringCellValue, "A2")
            assertCellEquals("1. step\n   [log] trace", sheet.getRow(1).getCell(7).stringCellValue, "H2")
            assertCellEquals("S-2", sheet.getRow(2).getCell(0).stringCellValue, "A3")
            assertCellEquals("plain scenario", sheet.getRow(2).getCell(7).stringCellValue, "H3")
        }
    }

    /**
     * Boundary-тест проверяет пустой snapshot: XLSX должен содержать только строку заголовков без data rows.
     */
    @Test
    fun `generate workbook from empty snapshot returns header only workbook`() {
        Mockito.`when`(columnConfigService.getConfig()).thenReturn(ColumnConfigProperties(emptyMap()))

        val workbook = XSSFWorkbook(ByteArrayInputStream(service.generateWorkbookFromSnapshot(emptyMap())))
        workbook.use {
            val sheet = it.getSheet("Test Report")
            assertEquals(0, sheet.lastRowNum, "Пустой snapshot должен создавать workbook только с header row")
            assertTrue(sheet.getRow(0).getCell(0).stringCellValue.isNotBlank(), "Header row должен быть заполнен даже для пустого snapshot")
        }
    }
}
