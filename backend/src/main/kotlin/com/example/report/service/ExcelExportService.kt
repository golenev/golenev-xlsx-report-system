package com.example.report.service

import com.example.report.dto.ScenarioAttachmentRequest
import com.example.report.dto.ScenarioParameterRequest
import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.RegionUtil
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

    /**
     * Формирует Excel-книгу с актуальным отчётом по тест-кейсам.
     */
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
                "scenario" to formatScenario(it.scenario),
                "notes" to it.notes,
            )
        }
        return renderWorkbook(rows, report.columnConfig)
    }

    /**
     * Формирует Excel-книгу на основе сохранённого снимка регресса.
     */
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

    /**
     * Рендерит набор строк отчёта в XLSX-файл с заголовками, стилями и шириной колонок из конфигурации.
     */
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
        val firstContinuationStyle = workbook.createCellStyle().apply {
            cloneStyleFrom(cellStyle)
            borderBottom = BorderStyle.NONE
        }
        val middleContinuationStyle = workbook.createCellStyle().apply {
            cloneStyleFrom(cellStyle)
            borderTop = BorderStyle.NONE
            borderBottom = BorderStyle.NONE
        }
        val lastContinuationStyle = workbook.createCellStyle().apply {
            cloneStyleFrom(cellStyle)
            borderTop = BorderStyle.NONE
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

        var nextRowIndex = 1
        rows.forEach { row ->
            val chunksByColumn = columnKeys.map { key -> splitToExcelCells(row[key]) }
            val rowSpan = chunksByColumn.maxOf { it.size }

            repeat(rowSpan) { rowOffset ->
                val sheetRow = sheet.createRow(nextRowIndex + rowOffset)
                chunksByColumn.forEachIndexed { cellIndex, chunks ->
                    val cell = sheetRow.createCell(cellIndex)
                    cell.setCellValue(chunks.getOrElse(rowOffset) { "" })
                    cell.cellStyle = when {
                        rowSpan == 1 -> cellStyle
                        rowOffset == 0 -> firstContinuationStyle
                        rowOffset == rowSpan - 1 -> lastContinuationStyle
                        else -> middleContinuationStyle
                    }
                }
            }

            chunksByColumn.forEachIndexed { cellIndex, chunks ->
                if (rowSpan > 1 && chunks.size == 1) {
                    val region = CellRangeAddress(nextRowIndex, nextRowIndex + rowSpan - 1, cellIndex, cellIndex)
                    sheet.addMergedRegion(region)
                    RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet)
                    RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet)
                    RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet)
                    RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet)
                }
            }
            nextRowIndex += rowSpan
        }

        ByteArrayOutputStream().use { outputStream ->
            workbook.write(outputStream)
            workbook.close()
            return outputStream.toByteArray()
        }
    }

    /**
     * Преобразует структурированный сценарий в многострочный человекочитаемый текст для ячейки Excel.
     */
    private fun formatScenario(scenario: ScenarioRequest?): String? {
        if (scenario == null) return null
        fun render(steps: List<ScenarioStepRequest>, level: Int): List<String> = steps.flatMap { step ->
            val indent = "   ".repeat(level)
            buildList {
                if (!step.text.isNullOrBlank()) add("$indent${step.text.trim()}")
                step.parameters.forEach { parameter ->
                    add("$indent   ${parameter.name.orEmpty()} — ${parameter.value.orEmpty()}")
                }
                step.attachments.orEmpty().forEach { attachment ->
                    val name = attachment.name.orEmpty().ifBlank { "Attachment" }
                    add("$indent   [$name] ${attachment.content.orEmpty()}")
                }
                addAll(render(step.subSteps, level + 1))
            }
        }
        return render(scenario.steps, 0).joinToString("\n").takeIf { it.isNotBlank() }
    }

    private fun splitToExcelCells(value: String?): List<String> {
        val source = value.orEmpty()
        if (source.isEmpty()) return listOf("")

        return buildList {
            var startIndex = 0
            while (startIndex < source.length) {
                var endIndex = minOf(startIndex + EXCEL_CELL_CHARACTER_LIMIT, source.length)
                if (
                    endIndex < source.length &&
                    Character.isHighSurrogate(source[endIndex - 1]) &&
                    Character.isLowSurrogate(source[endIndex])
                ) {
                    endIndex--
                }
                add(source.substring(startIndex, endIndex))
                startIndex = endIndex
            }
        }
    }

    /**
     * Нормализует сценарий из снимка регресса к строковому виду независимо от способа его хранения.
     */
    private fun formatSnapshotScenario(rawScenario: Any?): String? {
        return when (rawScenario) {
            null -> null
            is String -> {
                val trimmed = rawScenario.trim()
                if (trimmed.startsWith("{")) {
                    runCatching {
                        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                        formatScenario(mapper.readValue(trimmed, ScenarioRequest::class.java))
                    }.getOrElse { rawScenario }
                } else rawScenario
            }
            is ScenarioRequest -> formatScenario(rawScenario)
            is Map<*, *> -> {
                fun parseSteps(rawSteps: Any?): List<ScenarioStepRequest> = (rawSteps as? List<*>)
                    ?.mapNotNull { rawStep ->
                        val step = rawStep as? Map<*, *> ?: return@mapNotNull null
                        ScenarioStepRequest(
                            number = (step["number"] as? Number)?.toInt(),
                            text = step["text"] as? String,
                            attachments = (step["attachments"] as? List<*>)
                                ?.mapNotNull { rawAttachment ->
                                    val attachment = rawAttachment as? Map<*, *> ?: return@mapNotNull null
                                    ScenarioAttachmentRequest(
                                        name = attachment["name"] as? String ?: attachment["type"] as? String,
                                        mediaType = attachment["mediaType"] as? String,
                                        content = attachment["content"] as? String,
                                        source = attachment["source"] as? String,
                                        sizeBytes = (attachment["sizeBytes"] as? Number)?.toLong(),
                                    )
                                }
                                ?: emptyList(),
                            subSteps = parseSteps(step["subSteps"]),
                            durationMs = (step["durationMs"] as? Number)?.toLong(),
                            parameters = (step["parameters"] as? List<*>)
                                ?.mapNotNull { rawParameter ->
                                    val parameter = rawParameter as? Map<*, *> ?: return@mapNotNull null
                                    ScenarioParameterRequest(parameter["name"] as? String, parameter["value"]?.toString())
                                }.orEmpty(),
                        )
                    }
                    .orEmpty()
                val steps = parseSteps(rawScenario["steps"])
                if (steps.isEmpty()) return rawScenario.toString()
                formatScenario(ScenarioRequest(steps = steps))
            }
            else -> rawScenario.toString()
        }
    }

    /**
     * Извлекает тест-кейсы из payload-снимка регресса и приводит их к внутренней модели экспорта.
     */
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
                scenario = formatSnapshotScenario(map["scenario"]),
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

    private companion object {
        const val EXCEL_CELL_CHARACTER_LIMIT = 32_767
    }
}
