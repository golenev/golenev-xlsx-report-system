package com.example.report.repository

import com.example.report.entity.TestReportEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

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
}
