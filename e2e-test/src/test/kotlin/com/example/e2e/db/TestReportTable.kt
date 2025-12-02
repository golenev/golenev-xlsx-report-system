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
    val priority = text("priority")
    val scenario = text("scenario").nullable()
    val notes = text("notes").nullable()
    val run1Status = text("run_1_status").nullable()
    val run2Status = text("run_2_status").nullable()
    val run3Status = text("run_3_status").nullable()
    val run4Status = text("run_4_status").nullable()
    val run5Status = text("run_5_status").nullable()
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
    val priority: String,
    val scenario: String?,
    val notes: String?,
    val run1Status: String?,
    val run2Status: String?,
    val run3Status: String?,
    val run4Status: String?,
    val run5Status: String?,
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
    priority = row[TestReportTable.priority],
    scenario = row[TestReportTable.scenario],
    notes = row[TestReportTable.notes],
    run1Status = row[TestReportTable.run1Status],
    run2Status = row[TestReportTable.run2Status],
    run3Status = row[TestReportTable.run3Status],
    run4Status = row[TestReportTable.run4Status],
    run5Status = row[TestReportTable.run5Status],
    updatedAt = row[TestReportTable.updatedAt],
)
