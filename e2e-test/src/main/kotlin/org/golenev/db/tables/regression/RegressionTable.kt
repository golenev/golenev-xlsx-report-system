package org.golenev.db.tables.regression

import org.golenev.db.jsonbColumn
import org.golenev.restapi.endpoints.TestUpsertItem
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

object RegressionTable : Table("regressions") {
    val id = long("id").autoIncrement()
    val status = text("status")
    val regressionDate = date("regression_date")
    val releaseName = text("release_name")
    val payload = jsonbColumn<RegressionPayloadDto>("payload").nullable()

    override val primaryKey = PrimaryKey(id)
}

data class RegressionRow(
    val id: Long,
    val status: String,
    val regressionDate: LocalDate,
    val releaseName: String,
    val payload: RegressionPayloadDto?,
)

fun mapToRegression(row: ResultRow) = RegressionRow(
    id = row[RegressionTable.id],
    status = row[RegressionTable.status],
    regressionDate = row[RegressionTable.regressionDate],
    releaseName = row[RegressionTable.releaseName],
    payload = row[RegressionTable.payload],
)

data class RegressionPayloadDto(
    val regressionDate: String? = null,
    val status: String? = null,
    val releaseName: String? = null,
    val tests: List<TestUpsertItem>? = null,
)
