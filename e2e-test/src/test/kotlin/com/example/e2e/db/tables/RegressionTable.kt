package com.example.e2e.db.tables

import com.example.e2e.dto.TestUpsertItem
import com.example.e2e.utils.JsonUtils
import java.time.LocalDate
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.json.jsonb

object RegressionTable : Table("regressions") {
    val id = long("id").autoIncrement()
    val status = text("status")
    val regressionDate = date("regression_date")
    val releaseName = text("release_name")
    val payload = jsonbColumn<RegressionPayload>("payload").nullable()

    override val primaryKey = PrimaryKey(id)
}

data class RegressionRow(
    val id: Long,
    val status: String,
    val regressionDate: LocalDate,
    val releaseName: String,
    val payload: RegressionPayload?,
)

fun mapToRegression(row: ResultRow) = RegressionRow(
    id = row[RegressionTable.id],
    status = row[RegressionTable.status],
    regressionDate = row[RegressionTable.regressionDate],
    releaseName = row[RegressionTable.releaseName],
    payload = row[RegressionTable.payload],
)

data class RegressionPayload(
    val regressionDate: String? = null,
    val status: String? = null,
    val releaseName: String? = null,
    val tests: List<TestUpsertItem>? = null,
)

inline fun <reified T : Any> Table.jsonbColumn(name: String): Column<T> =
    jsonb(name, { JsonUtils.globalMapper.writeValueAsString(it) }, { JsonUtils.globalMapper.readValue(it, T::class.java) })
