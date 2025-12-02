package com.example.report.service

import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class ExcelExportService(
    private val testReportService: TestReportService
) {
    private val columnKeys = listOf(
        "testId",
        "category",
        "shortTitle",
        "issueLink",
        "readyDate",
        "generalStatus",
        "priority",
        "scenario",
        "notes"
    )

    fun generateWorkbook(): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Test Report")

        val headerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.index)
        }
        val cellStyle = workbook.createCellStyle().apply {
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            wrapText = true
        }

        val report = testReportService.getReport()
        val headers = listOf(
            "Test ID",
            "Category / Feature",
            "Short Title",
            "YouTrack Issue Link",
            "Ready Date",
            "General Test Status",
            "Priority",
            "Detailed Scenario",
            "Notes"
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { idx, title ->
            val cell = headerRow.createCell(idx)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
            val key = columnKeys.getOrNull(idx)
            key?.let { report.columnConfig[it] }?.let { width ->
                sheet.setColumnWidth(idx, width * 40)
            }
        }

        report.items.forEachIndexed { rowIndex, item ->
            val row = sheet.createRow(rowIndex + 1)
            val values = listOf(
                item.testId,
                item.category,
                item.shortTitle,
                item.issueLink,
                item.readyDate?.toString(),
                item.generalStatus,
                item.priority,
                item.scenario,
                item.notes
            )
            values.forEachIndexed { cellIndex, value ->
                val cell = row.createCell(cellIndex)
                cell.setCellValue(value ?: "")
                cell.cellStyle = cellStyle
            }
        }

        ByteArrayOutputStream().use { outputStream ->
            workbook.write(outputStream)
            workbook.close()
            return outputStream.toByteArray()
        }
    }
}
