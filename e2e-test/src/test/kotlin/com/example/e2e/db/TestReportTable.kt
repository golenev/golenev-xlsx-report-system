package com.example.e2e.db

import java.time.LocalDate
import java.time.OffsetDateTime
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone


object TestReportTable : Table("test_report") {
    val id = long("id").autoIncrement()
    val testId = text("test_id")
    val category = text("category").nullable()
    val shortTitle = text("short_title").nullable()
    val issueLink = text("issue_link").nullable()
    val readyDate = date("ready_date").nullable()
    val generalStatus = text("general_status").nullable()
    val scenario = text("scenario").nullable()
    val notes = text("notes").nullable()
    val regressionStatus = text("regression_status").nullable()
    val regressionDate = date("regression_date").nullable()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

data class TestReportRow(
    val id: Long,
    val testId: String,
    val category: String?,
    val shortTitle: String?,
    val issueLink: String?,
    val readyDate: LocalDate?,
    val generalStatus: String?,
    val scenario: String?,
    val notes: String?,
    val regressionStatus: String?,
    val regressionDate: LocalDate?,
    val updatedAt: OffsetDateTime?,
)

fun mapToTestReport(row: ResultRow): TestReportRow = TestReportRow(
    id = row[TestReportTable.id],
    testId = row[TestReportTable.testId],
    category = row[TestReportTable.category],
    shortTitle = row[TestReportTable.shortTitle],
    issueLink = row[TestReportTable.issueLink],
    readyDate = row[TestReportTable.readyDate],
    generalStatus = row[TestReportTable.generalStatus],
    scenario = row[TestReportTable.scenario],
    notes = row[TestReportTable.notes],
    regressionStatus = row[TestReportTable.regressionStatus],
    regressionDate = row[TestReportTable.regressionDate],
    updatedAt = row[TestReportTable.updatedAt],
)

object RegressionTable : Table("regressions") {
    val id = long("id").autoIncrement()
    val regressionDate = date("regression_date")
    val payload = text("payload")

    override val primaryKey = PrimaryKey(id)
}
