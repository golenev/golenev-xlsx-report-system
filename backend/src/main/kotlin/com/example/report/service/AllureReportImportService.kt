package com.example.report.service

import com.example.report.service.allure.TestCaseModel
import com.example.report.service.allure.parseAllureReports
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
@Service
class AllureReportImportService {

    fun loadTestCases(files: List<MultipartFile>): List<TestCaseModel> {
        val fileContents = files.map { file ->
            val name = file.originalFilename ?: "file.json"
            val content = file.inputStream.bufferedReader().use { it.readText() }
            name to content
        }

        return parseAllureReports(fileContents)
    }
}
