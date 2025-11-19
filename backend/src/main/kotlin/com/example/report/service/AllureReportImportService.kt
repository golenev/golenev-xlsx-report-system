package com.example.report.service

import com.example.report.service.allure.TestCaseModel
import com.example.report.service.allure.parseAllureReportsFromFolder
import org.springframework.stereotype.Service

@Service
class AllureReportImportService {

    fun loadTestCases(path: String): List<TestCaseModel> {
        return parseAllureReportsFromFolder(path)
    }
}
