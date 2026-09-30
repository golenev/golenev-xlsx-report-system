package org.golenev.db.tables.testReportTable

import org.golenev.db.jsonbColumn
import org.golenev.restapi.endpoints.ScenarioRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.LocalDate
import java.time.OffsetDateTime

object TestReportTable : Table("test_report") {
    val id = long("id").autoIncrement()
    val testId = text("test_id")
    val category = text("category").nullable()
    val shortTitle = text("short_title").nullable()
    val issueLink = text("issue_link").nullable()
    val readyDate = date("ready_date").nullable()
    val generalStatus = text("general_status").nullable()
    val priority = text("priority")
    val scenario = jsonbColumn<ScenarioRequest>("scenario").nullable()
    val notes = text("notes").nullable()
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
    val scenario: ScenarioRequest?,
    val notes: String?,
    val updatedAt: OffsetDateTime?,
)

fun mapToTestReport(row: ResultRow): TestReportRow =
    TestReportRow(
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
        updatedAt = row[TestReportTable.updatedAt],
    )
