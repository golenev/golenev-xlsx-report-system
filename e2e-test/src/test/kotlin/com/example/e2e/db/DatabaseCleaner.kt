package com.example.e2e.db

import java.time.LocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere

object DatabaseCleaner {

    fun deleteReportsByDate(date: LocalDate = LocalDate.now()) = dbReportExec {
        TestReportTable.deleteWhere { TestReportTable.readyDate eq date }
    }
}
