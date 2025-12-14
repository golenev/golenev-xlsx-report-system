package com.example.e2e.utils

import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import net.datafaker.Faker
import java.util.*


object TestDataGenerator {

    private val faker = Faker(Locale("ru"))

    fun generateTestCases(count: Int = 20, readyDate: String): List<TestUpsertItem> {
        return (1..count).map { index ->
            TestUpsertItem(
                readyDate = readyDate,
                testId = generateTestId(index),
                category = faker.commerce().department(),
                shortTitle = faker.app().name(),
                scenario = "${faker.chuckNorris().fact()}\\n2 ${faker.chuckNorris().fact()}\\n3 ${
                    faker.chuckNorris().fact()
                }\\n4"
            )
        }
    }

    fun generateBatch(count: Int = 20, readyDate: String): TestBatchRequest =
        TestBatchRequest(items = generateTestCases(count, readyDate = readyDate))

    fun seedRandomReportData(
        reportService: ReportService = ReportService(),
        count: Int = 20,
        readyDate: String
    ): List<TestUpsertItem> {
        val items = generateTestCases(count, readyDate)
        reportService.sendBatch(TestBatchRequest(items))
        return items
    }

    private fun generateTestId(index: Int): String =
        "TC-${faker.number().digits(5)}-$index"
}

