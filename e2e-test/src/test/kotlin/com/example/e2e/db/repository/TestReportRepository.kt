package com.example.e2e.db.repository

import com.example.e2e.db.dbReportExec
import com.example.e2e.db.tables.TestReportTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import java.time.LocalDate

object TestReportRepository {

    fun deleteReportsByDate(date: LocalDate = LocalDate.now()) = dbReportExec {
        TestReportTable.deleteWhere { TestReportTable.readyDate eq date }
    }

    fun deleteByTestId(testId: String) = dbReportExec {
        TestReportTable.deleteWhere { TestReportTable.testId eq testId }
    }

    fun countByTestId(testId: String): Long = dbReportExec {
        TestReportTable.select { TestReportTable.testId eq testId }.count()
    }
}
