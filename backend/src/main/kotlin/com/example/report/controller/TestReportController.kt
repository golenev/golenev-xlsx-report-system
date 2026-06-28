package com.example.report.controller

import com.example.report.dto.TestUpsertItem
import com.example.report.dto.TestBatchRequest
import com.example.report.dto.RegressionStartRequest
import com.example.report.dto.RegressionStopRequest
import com.example.report.service.ColumnConfigService
import com.example.report.service.ExcelExportService
import com.example.report.service.RegressionService
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
    private val regressionService: RegressionService,
) {

    /**
     * Возвращает текущий список тест-кейсов вместе с настройками колонок и переводами для UI.
     */
    @GetMapping("/tests")
    fun getTests() = testReportService.getReport()

    /**
     * Создаёт или обновляет один тест-кейс по данным из тела запроса.
     */
    @PostMapping("/tests")
    fun upsertTest(
        @RequestParam(defaultValue = "false") forceUpdate: Boolean,
        @Valid @RequestBody request: TestUpsertItem,
    ) {
        testReportService.upsertTest(request, forceUpdate)
    }

    /**
     * Массово создаёт или обновляет тест-кейсы; при активном регрессе проверяет его наличие и синхронизирует статусы прогона.
     */
    @PostMapping("/tests/batch")
    fun upsertBatch(
        @RequestParam(defaultValue = "false") isRegressRunning: Boolean,
        @RequestParam(defaultValue = "false") forceUpdate: Boolean,
        @Valid @RequestBody request: TestBatchRequest,
    ) {
        if (isRegressRunning) {
            regressionService.requireRunningRegression()
        }

        testReportService.upsertBatch(request, isRegressRunning, forceUpdate)
    }

    /**
     * Удаляет тест-кейс по его идентификатору.
     */
    @DeleteMapping("/tests/{testId}")
    fun deleteTest(@PathVariable testId: String) {
        testReportService.deleteTest(testId)
    }

    /**
     * Формирует Excel-файл с актуальным отчётом по тест-кейсам и возвращает его как вложение для скачивания.
     */
    @GetMapping("/tests/export/excel")
    fun exportExcel(): ResponseEntity<ByteArray> {
        val body = excelExportService.generateWorkbook()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tests.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body)
    }

    /**
     * Возвращает конфигурацию колонок отчёта для клиентского приложения.
     */
    @GetMapping("/config/columns")
    fun getColumnConfig() = columnConfigService.getConfig()

    /**
     * Возвращает состояние текущего или последнего запущенного регресса за сегодня.
     */
    @GetMapping("/regressions/current")
    fun getCurrentRegression() = regressionService.getTodayState()

    /**
     * Запускает новый регресс для указанного релиза.
     */
    @PostMapping("/regressions/start")
    fun startRegression(@Valid @RequestBody request: RegressionStartRequest) =
        regressionService.startRegression(request)

    /**
     * Завершает активный регресс и сохраняет снимок результатов по всем тест-кейсам.
     */
    @PostMapping("/regressions/stop")
    fun stopRegression(@Valid @RequestBody request: RegressionStopRequest) =
        regressionService.stopRegression(request)

    /**
     * Отменяет активный регресс: удаляет пустой запуск или закрывает запуск с уже накопленными данными.
     */
    @PostMapping("/regressions/cancel")
    fun cancelRegression() = regressionService.cancelRegression()

    /**
     * Возвращает список релизов, для которых создавались регрессионные прогоны.
     */
    @GetMapping("/regressions/releases")
    fun getRegressionReleases() = regressionService.listReleases()

    /**
     * Возвращает сохранённый снимок конкретного регресса по его идентификатору.
     */
    @GetMapping("/regressions/{regressionId}")
    fun getRegressionSnapshot(@PathVariable regressionId: Long) =
        regressionService.getRegressionSnapshot(regressionId)

    /**
     * Формирует Excel-файл из снимка регресса и возвращает его как вложение для скачивания.
     */
    @GetMapping("/regressions/{regressionId}/snapshot.xlsx")
    fun downloadRegressionSnapshot(@PathVariable regressionId: Long): ResponseEntity<ByteArray> {
        val body = regressionService.getRegressionSnapshotWorkbook(regressionId)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=regression-$regressionId.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body)
    }
}
