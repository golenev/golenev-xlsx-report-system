package com.example.report.dto

import com.example.report.model.RegressionStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

private val allowedRegressionResults = setOf("PASSED", "FAILED", "SKIPPED")

/**
 * Валидирует и нормализует результаты регресса из запроса завершения прогона.
 * Допускает только статусы `PASSED`, `FAILED` и `SKIPPED`, приводя значения к верхнему регистру.
 */
fun validateRegressionResults(results: Map<String, String>): Map<String, String> {
    return results.mapValues { (testId, value) ->
        val normalized = value.trim().uppercase()
        if (normalized.isEmpty()) {
            throw IllegalArgumentException("Regression status for $testId is empty")
        }
        if (normalized !in allowedRegressionResults) {
            throw IllegalArgumentException("Invalid regression status $value for $testId")
        }
        normalized
    }
}

/**
 * Тело запроса для завершения регресса.
 *
 * Аннотация `@field:NotEmpty` требует, чтобы карта результатов была передана и содержала хотя бы один элемент.
 *
 * @property results карта статусов регресса, где ключ — `testId`, а значение — статус `PASSED`, `FAILED` или `SKIPPED`.
 */
data class RegressionStopRequest(
    @field:NotEmpty
    val results: Map<String, String>,
)

/**
 * Тело запроса для запуска нового регресса.
 *
 * Аннотация `@field:NotBlank` требует непустое имя релиза после удаления пробелов по краям.
 *
 * @property releaseName человекочитаемое имя релиза, по которому нельзя запускать дублирующий регресс.
 */
data class RegressionStartRequest(
    @field:NotBlank
    val releaseName: String,
)

/**
 * Ответ с текущим состоянием регресса.
 *
 * @property status состояние регресса: `RUNNING`, `COMPLETED` или `IDLE`.
 * @property regressionDate дата регресса строкой, чтобы API стабильно отдавал дату без timezone-преобразований.
 * @property results уже накопленные результаты запущенного регресса в формате `testId -> regressionStatus`.
 * @property releaseName имя релиза для активного или сохранённого регресса.
 */
data class RegressionStateResponse(
    val status: RegressionStatus,
    val regressionDate: String?,
    val results: Map<String, String> = emptyMap(),
    val releaseName: String? = null,
)

/**
 * Краткая запись для списка сохранённых регрессионных релизов.
 *
 * @property id идентификатор регресса, который используется для запроса snapshot и скачивания XLSX.
 * @property name имя релиза.
 * @property regressionDate дата запуска или сохранения регресса.
 * @property status итоговый статус записи регресса.
 */
data class RegressionReleaseSummary(
    val id: Long,
    val name: String,
    val regressionDate: String?,
    val status: RegressionStatus,
)

/**
 * Ответ с сохранённым snapshot конкретного регресса.
 *
 * @property id идентификатор записи регресса.
 * @property name имя релиза.
 * @property status статус регресса.
 * @property regressionDate дата регресса.
 * @property snapshot сохранённый payload с метаданными релиза, тестами и статусами прогона.
 */
data class RegressionSnapshotResponse(
    val id: Long,
    val name: String,
    val status: RegressionStatus,
    val regressionDate: String?,
    val snapshot: Map<String, Any?>,
)
