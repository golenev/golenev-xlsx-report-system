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
    private val testReportService: TestReportService,
    private val columnConfigService: ColumnConfigService,
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
        val report = testReportService.getReport()
        val rows = report.items.map {
            mapOf(
                "testId" to it.testId,
                "category" to it.category,
                "shortTitle" to it.shortTitle,
                "issueLink" to it.issueLink,
                "readyDate" to it.readyDate?.toString(),
                "generalStatus" to it.generalStatus,
                "priority" to it.priority,
                "scenario" to it.scenario,
                "notes" to it.notes,
            )
        }
        return renderWorkbook(rows, report.columnConfig)
    }

    fun generateWorkbookFromSnapshot(snapshot: Map<String, Any?>): ByteArray {
        val columnConfig = columnConfigService.getConfig().columns
        val tests = extractTestsFromSnapshot(snapshot)
        val rows = tests.map {
            mapOf(
                "testId" to it.testId,
                "category" to it.category,
                "shortTitle" to it.shortTitle,
                "issueLink" to it.issueLink,
                "readyDate" to it.readyDate,
                "generalStatus" to it.generalStatus,
                "priority" to it.priority,
                "scenario" to it.scenario,
                "notes" to it.notes,
            )
        }
        return renderWorkbook(rows, columnConfig)
    }

    private fun renderWorkbook(
        rows: List<Map<String, String?>>,
        columnConfig: Map<String, Int>
    ): ByteArray {
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
            key?.let { columnConfig[it] }?.let { width ->
                sheet.setColumnWidth(idx, width * 40)
            }
        }

        rows.forEachIndexed { rowIndex, row ->
            val sheetRow = sheet.createRow(rowIndex + 1)
            val values = columnKeys.map { key -> row[key] }
            values.forEachIndexed { cellIndex, value ->
                val cell = sheetRow.createCell(cellIndex)
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

    private fun extractTestsFromSnapshot(snapshot: Map<String, Any?>): List<SnapshotTest> {
        val tests = snapshot["tests"] as? List<*> ?: return emptyList()
        return tests.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val testId = map["testId"] as? String ?: return@mapNotNull null
            SnapshotTest(
                testId = testId,
                category = map["category"] as? String,
                shortTitle = map["shortTitle"] as? String,
                issueLink = map["issueLink"] as? String,
                readyDate = map["readyDate"]?.toString(),
                generalStatus = map["generalStatus"] as? String,
                priority = map["priority"]?.toString(),
                scenario = map["scenario"] as? String,
                notes = map["notes"] as? String,
            )
        }
    }

    private data class SnapshotTest(
        val testId: String,
        val category: String?,
        val shortTitle: String?,
        val issueLink: String?,
        val readyDate: String?,
        val generalStatus: String?,
        val priority: String?,
        val scenario: String?,
        val notes: String?,
    )
}
