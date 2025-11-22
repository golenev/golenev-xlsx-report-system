package com.example.report.handler

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

@RestControllerAdvice
class ApiExceptionHandler {

    private val missingFieldPattern = Regex("Required field (\\w+) is missing")

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        val status = ex.statusCode
        val reason = ex.reason
        val missingField = reason?.let { missingFieldPattern.find(it)?.groupValues?.getOrNull(1) }

        val body = buildMap<String, Any?> {
            put("timestamp", OffsetDateTime.now())
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
