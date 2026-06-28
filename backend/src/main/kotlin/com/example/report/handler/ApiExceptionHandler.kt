package com.example.report.handler

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.OffsetDateTime

/**
 * Глобальный обработчик API-ошибок для REST-контроллеров.
 *
 * Аннотация `@RestControllerAdvice` подключает класс как общий advice для контроллеров:
 * Spring вызывает методы с `@ExceptionHandler`, когда endpoint выбрасывает поддерживаемое исключение.
 */
@RestControllerAdvice
class ApiExceptionHandler(
    private val clock: Clock,
) {

    private val missingFieldPattern = Regex("Required field (\\w+) is missing")

    /**
     * Преобразует `ResponseStatusException` в единый JSON-ответ с HTTP-статусом, сообщением, путём запроса и временем.
     * Если сообщение соответствует шаблону пропущенного обязательного поля, добавляет в тело ответа `missingField`.
     *
     * Аннотация `@ExceptionHandler(ResponseStatusException::class)` говорит Spring вызывать этот метод
     * для исключений `ResponseStatusException`, выброшенных из контроллеров или сервисов во время обработки запроса.
     */
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        val status = ex.statusCode
        val reason = ex.reason
        val missingField = reason?.let { missingFieldPattern.find(it)?.groupValues?.getOrNull(1) }

        val body = buildMap<String, Any?> {
            put("timestamp", OffsetDateTime.now(clock))
            put("status", status.value())
            put("error", ex.cause)
            put("message", reason)
            put("path", request.requestURI)
            if (missingField != null) {
                put("missingField", missingField)
            }
        }

        return ResponseEntity.status(status).body(body)
    }
}
