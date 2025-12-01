package com.example.e2e.utils

import com.example.e2e.dto.GeneralTestStatus
import com.example.e2e.dto.TestBatchRequest
import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.service.ReportService
import java.time.LocalDate
import java.util.Locale
import net.datafaker.Faker

object TestDataGenerator {

    private val faker = Faker(Locale("ru"))
    private val generalStatuses = GeneralTestStatus.entries.map { it.value }.toTypedArray()

    fun generateTestCases(count: Int = 20, readyDate: LocalDate = LocalDate.now()): List<TestUpsertItem> {
        return (1..count).map { index ->
            TestUpsertItem(
                testId = generateTestId(index),
                category = faker.commerce().department(),
                shortTitle = faker.app().name(),
                issueLink = faker.internet().url(),
                readyDate = readyDate.toString(),
                generalStatus = faker.options().option(*generalStatuses),
                scenario = faker.lorem().sentence(8),
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
