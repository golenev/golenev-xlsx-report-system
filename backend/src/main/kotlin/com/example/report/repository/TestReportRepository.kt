package com.example.report.repository

import com.example.report.entity.TestReportEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

/**
 * Spring Data JPA repository для строк тестового отчёта.
 *
 * Реализацию интерфейса генерирует Spring Data JPA на старте приложения: `JpaRepository`
 * даёт стандартные CRUD-операции, а дополнительные методы создают запросы из имени метода.
 */
interface TestReportRepository : JpaRepository<TestReportEntity, Long> {
    /**
     * Ищет тест-кейс по внешнему идентификатору `testId`.
     *
     * Spring Data формирует запрос по имени метода: `findByTestId` означает фильтр по полю `testId`.
     */
    fun findByTestId(testId: String): Optional<TestReportEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from TestReportEntity report where report.testId = :testId")
    fun findForUpdateByTestId(@Param("testId") testId: String): Optional<TestReportEntity>
}
