package com.example.e2e.db.repository

import com.example.e2e.db.dbReportExec
import com.example.e2e.db.tables.RegressionRow
import com.example.e2e.db.tables.RegressionTable
import com.example.e2e.db.tables.mapToRegression
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select

object RegressionRepository {

    fun findByReleaseName(releaseName: String): RegressionRow? = dbReportExec {
        RegressionTable
            .select { RegressionTable.releaseName eq releaseName }
            .map(::mapToRegression)
            .singleOrNull()
    }

    fun deleteByReleaseName(releaseName: String) = dbReportExec {
        RegressionTable.deleteWhere { RegressionTable.releaseName eq releaseName }
    }
}
