package org.golenev.db.tables.regression

import org.golenev.db.dbReportExec
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

object RegressionDao {

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