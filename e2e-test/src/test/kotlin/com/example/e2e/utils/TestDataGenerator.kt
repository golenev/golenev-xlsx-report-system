package com.example.e2e.utils

import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.Priority
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import com.example.e2e.utils.TestDataGenerator.generateTestCases
import io.kotest.assertions.print.printWithType
import java.time.LocalDate
import java.util.Locale
import net.datafaker.Faker


object TestDataGenerator {

    private val faker = Faker(Locale("ru"))

    fun generateTestCases(count: Int = 20, readyDate: LocalDate = LocalDate.now()): List<TestUpsertItem> {
        return (1..count).map { index ->
            TestUpsertItem(
                testId = generateTestId(index),
                category = faker.commerce().department(),
                shortTitle = faker.app().name(),
                issueLink = faker.internet().url(),
                readyDate = readyDate.toString(),
                generalStatus = GeneralTestStatus.entries.map { it.value }.random(),
                priority = Priority.random().value,
                scenario = "${faker.chuckNorris().fact()}\\n2 ${faker.chuckNorris().fact()}\\n3 ${
                    faker.chuckNorris().fact()
                }\\n4",
                notes = faker.lorem().sentence(6),
            )
        }
    }

    fun generateBatch(count: Int = 20, readyDate: LocalDate = LocalDate.now()): TestBatchRequest =
        TestBatchRequest(items = generateTestCases(count, readyDate))

    fun seedRandomReportData(
        reportService: ReportService = ReportService(),
        count: Int = 20,
        readyDate: LocalDate = LocalDate.now(),
    ): List<TestUpsertItem> {
        val items = generateTestCases(count, readyDate)
        reportService.sendBatch(TestBatchRequest(items))
        return items
    }

    private fun generateTestId(index: Int): String =
        "TC-${faker.number().digits(5)}-$index"
}

