package com.example.e2e.db

import java.time.LocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import com.example.e2e.db.RegressionTable

object DatabaseCleaner {

    fun deleteReportsByDate(date: LocalDate = LocalDate.now()) = dbReportExec {
        TestReportTable.deleteWhere { TestReportTable.readyDate eq date }
    }

    fun deleteRegressionByDate(date: LocalDate = LocalDate.now()) = dbReportExec {
        RegressionTable.deleteWhere { RegressionTable.regressionDate eq date }
    }
}
