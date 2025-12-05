package com.example.e2e.db.repository

import com.example.e2e.db.dbReportExec
import com.example.e2e.db.tables.RegressionRow
import com.example.e2e.db.tables.RegressionTable
import com.example.e2e.db.tables.mapToRegression
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

object RegressionRepository {

    fun findByReleaseName(releaseName: String): RegressionRow? = dbReportExec {
        RegressionTable
            .selectAll().where { RegressionTable.releaseName eq releaseName }
            .map { mapToRegression(it) }
            .firstOrNull()
    }

    fun deleteByReleaseName(releaseName: String) = dbReportExec {
        RegressionTable.deleteWhere { RegressionTable.releaseName eq releaseName }
    }
}
