package com.example.report.controller

import com.example.report.dto.TestUpsertItem
import com.example.report.dto.TestBatchRequest
import com.example.report.service.AllureReportImportService
import com.example.report.service.ColumnConfigService
import com.example.report.service.ExcelExportService
import com.example.report.service.TestReportService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TestReportController(
    private val testReportService: TestReportService,
    private val excelExportService: ExcelExportService,
    private val columnConfigService: ColumnConfigService,
    private val allureReportImportService: AllureReportImportService
) {

    @GetMapping("/tests")
    fun getTests() = testReportService.getReport()

    @PostMapping("/tests")
    fun upsertTest(@Valid @RequestBody request: TestUpsertItem) {
        testReportService.upsertTest(request)
    }

    @PostMapping("/tests/batch")
    fun upsertBatch(@Valid @RequestBody request: TestBatchRequest) {
        testReportService.upsertBatch(request)
    }

    @DeleteMapping("/tests/{testId}")
    fun deleteTest(@PathVariable testId: String) {
        testReportService.deleteTest(testId)
    }

    @GetMapping("/tests/export/excel")
    fun exportExcel(): ResponseEntity<ByteArray> {
        val body = excelExportService.generateWorkbook()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tests.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body)
    }

    @GetMapping("/config/columns")
    fun getColumnConfig() = columnConfigService.getConfig()

    @GetMapping("/allure/test-cases")
    fun getAllureTestCases(@RequestParam path: String) =
        allureReportImportService.loadTestCases(path)
}
