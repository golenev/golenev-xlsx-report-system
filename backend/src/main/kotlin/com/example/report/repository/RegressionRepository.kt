package com.example.report.repository

import com.example.report.entity.RegressionEntity
import com.example.report.model.RegressionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * Spring Data JPA repository для таблицы регрессов.
 *
 * Явной реализации у интерфейса нет в кодовой базе: Spring Data JPA создаёт proxy-реализацию во время старта приложения.
 * Базовые CRUD-методы приходят из `JpaRepository`, а методы ниже превращаются в запросы по их именам
 * (query derivation): Spring разбирает имя метода, сопоставляет части имени с полями `RegressionEntity`
 * и строит SQL/JPQL-запрос к базе данных.
 */
interface RegressionRepository : JpaRepository<RegressionEntity, Long> {
    /**
     * Ищет регресс по точному имени релиза.
     *
     * Spring Data формирует запрос по имени метода: `findByReleaseName` означает фильтр по полю `releaseName`.
     */
    fun findByReleaseName(releaseName: String): Optional<RegressionEntity>

    /**
     * Возвращает самый свежий регресс с указанным статусом.
     *
     * Spring Data читает имя как: найти первую запись по `status`, отсортировать по `regressionDate` по убыванию.
     */
    fun findFirstByStatusOrderByRegressionDateDesc(status: RegressionStatus): RegressionEntity?

    /**
     * Возвращает все регрессы, отсортированные по дате регресса от новых к старым.
     *
     * Spring Data формирует сортировку из части имени `OrderByRegressionDateDesc`.
     */
    fun findAllByOrderByRegressionDateDesc(): List<RegressionEntity>
}
