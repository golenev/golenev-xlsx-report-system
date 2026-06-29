package com.example.report.controller

import com.example.report.dto.TestBatchRequest
import com.example.report.dto.TestUpsertItem
import com.example.report.service.RegressionService
import com.example.report.service.TestReportService
import helpers.AllureUpload
import helpers.parseAllureReportsFromUploads
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@RestController
class UploadReportController(
    private val testReportService: TestReportService,
    private val regressionService: RegressionService,
) {

    /**
     * Принимает multipart-файлы Allure-отчёта, парсит из них тест-кейсы и передаёт их на сохранение.
     * Если загрузка выполняется во время регресса, дополнительно проверяет, что регресс действительно запущен.
     *
     * @param isRegressRunning request-параметр, который включает режим регресса: при `true` из отчёта берётся
     * `runStatus`, статусы синхронизируются с текущим регрессом, а перед импортом проверяется наличие активного прогона.
     * @param forceUpdate request-параметр, который разрешает перезаписывать ручные поля существующих тест-кейсов
     * (`issueLink`, `generalStatus`, `priority`, `notes`) и явно переданную `readyDate`; при `false` эти поля сохраняются.
     * @param files multipart request-параметр со всеми файлами выбранной директории `allure-results`, включая JSON-результаты и вложения.
     * @param paths опциональный multipart request-параметр с относительными путями файлов; элемент `paths[i]`
     * соответствует `files[i]` и помогает парсеру сопоставлять JSON-результаты с attachment-файлами.
     */
    @PostMapping("/uploadReport", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadReport(
        @RequestParam(defaultValue = "false") isRegressRunning: Boolean,
        @RequestParam(defaultValue = "false") forceUpdate: Boolean,
        @RequestParam("files") files: List<MultipartFile>,
        @RequestParam("paths", required = false) paths: List<String>?,
    ) {
        if (files.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Не загружены файлы отчёта")
        }

        if (isRegressRunning) {
            regressionService.requireRunningRegression()
        }

        val uploads = files.mapIndexed { index, file ->
            val fallbackName = file.originalFilename ?: "file-$index.json"
            val path = paths?.getOrNull(index)?.takeIf { it.isNotBlank() } ?: fallbackName
            AllureUpload(path = path, content = file.bytes)
        }

        val parsedCases = try {
            parseAllureReportsFromUploads(uploads)
        } catch (ex: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "Ошибка парсинга отчёта", ex)
        }

        val request = TestBatchRequest(
            items = parsedCases.map { testCase ->
                TestUpsertItem(
                    testId = testCase.id,
                    category = testCase.category,
                    shortTitle = testCase.name,
                    scenario = testReportService.buildScenarioFromText(testCase.scenario),
                    runStatus = testCase.runStatus,
                )
            },
        )

        testReportService.upsertBatch(request, isRegressRunning, forceUpdate)
    }
}
