package com.example.e2e.db.tables

import java.time.LocalDate
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object RegressionTable : Table("regressions") {
    val id = long("id").autoIncrement()
    val status = text("status")
    val regressionDate = date("regression_date")
    val releaseName = text("release_name")
    val payload = text("payload").nullable()

    override val primaryKey = PrimaryKey(id)
}

data class RegressionRow(
    val id: Long,
    val status: String,
    val regressionDate: LocalDate,
    val releaseName: String,
    val payload: String?,
)

fun mapToRegression(row: ResultRow) = RegressionRow(
    id = row[RegressionTable.id],
    status = row[RegressionTable.status],
    regressionDate = row[RegressionTable.regressionDate],
    releaseName = row[RegressionTable.releaseName],
    payload = row[RegressionTable.payload],
)
