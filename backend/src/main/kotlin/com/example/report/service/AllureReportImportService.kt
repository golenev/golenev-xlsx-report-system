package com.example.report.service

import com.example.report.service.allure.TestCaseModel
import com.example.report.service.allure.parseAllureReportsFromFolder
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.File

@Service
class AllureReportImportService {

    fun loadTestCases(path: String): List<TestCaseModel> {
        val folder = File(path)
        if (!folder.exists() || !folder.isDirectory) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Папка с тестами Allure не найдена по пути: $path"
            )
        }

        return try {
            parseAllureReportsFromFolder(path)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "Некорректный путь к отчетам")
        } catch (ex: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "Ошибка при обработке отчетов Allure")
        }
    }
}
