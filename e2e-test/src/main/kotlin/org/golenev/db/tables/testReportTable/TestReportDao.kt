package org.golenev.db.tables.testReportTable

import org.golenev.db.dbReportExec
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.LocalDate

object TestReportDao {

    fun truncate() = dbReportExec {
        TransactionManager.current().exec("TRUNCATE TABLE ${TestReportTable.tableName} RESTART IDENTITY CASCADE")
    }

    fun deleteReportsByDate(date: LocalDate = LocalDate.now()) = dbReportExec {
        TestReportTable.deleteWhere { TestReportTable.readyDate eq date }
    }

    fun deleteByTestId(testId: String) = dbReportExec {
        TestReportTable.deleteWhere { TestReportTable.testId eq testId }
    }

    fun countByTestId(testId: String): Long = dbReportExec {
        TestReportTable.select { TestReportTable.testId eq testId }
            .count()
    }
}