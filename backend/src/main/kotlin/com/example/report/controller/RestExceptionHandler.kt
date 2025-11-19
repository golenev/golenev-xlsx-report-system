package com.example.report.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.server.ResponseStatusException
import jakarta.servlet.http.HttpServletRequest

@RestControllerAdvice
class RestExceptionHandler {

    data class ApiError(
        val status: Int,
        val message: String,
        val path: String
    )

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val status = ex.statusCode
        val error = ApiError(status.value(), ex.reason ?: ex.message ?: "Unexpected error", request.requestURI)
        return ResponseEntity(error, status)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val status = HttpStatus.METHOD_NOT_ALLOWED
        val message = ex.message ?: "Method not supported"
        val error = ApiError(status.value(), message, request.requestURI)
        return ResponseEntity(error, status)
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFound(
        ex: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val status = HttpStatus.NOT_FOUND
        val message = "Endpoint not found: ${ex.requestURL}"
        val error = ApiError(status.value(), message, request.requestURI)
        return ResponseEntity(error, status)
    }
}
