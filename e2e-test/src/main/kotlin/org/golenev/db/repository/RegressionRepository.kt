package org.golenev.db.repository

import org.golenev.db.dbReportExec
import org.golenev.db.tables.RegressionRow
import org.golenev.db.tables.RegressionTable
import org.golenev.db.tables.mapToRegression
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

object RegressionRepository {

    fun findByReleaseName(releaseName: String): RegressionRow? =
       dbReportExec {
            RegressionTable
                .selectAll()
                .where { RegressionTable.releaseName eq releaseName }
                .map { mapToRegression(it) }
                .firstOrNull()
        }

    fun deleteByReleaseName(releaseName: String) = dbReportExec {
        RegressionTable.deleteWhere { RegressionTable.releaseName eq releaseName }
    }
}
