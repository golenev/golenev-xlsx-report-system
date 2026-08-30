package com.example.report.handler

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ApiExceptionHandlerUnitTest {

    private val clock = Clock.fixed(Instant.parse("2026-06-28T10:15:30Z"), ZoneOffset.UTC)
    private val handler = ApiExceptionHandler(clock)
    private val request: HttpServletRequest = Mockito.mock(HttpServletRequest::class.java).also {
        Mockito.`when`(it.requestURI).thenReturn("/api/tests")
    }

    @Test
    fun `required field error contains stable body and extracted missing field`() {
        val cause = IllegalArgumentException("root cause")
        val exception = ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Required field shortTitle is missing",
            cause,
        )

        val response = handler.handleResponseStatusException(exception, request)
        val body = response.body.orEmpty()

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(OffsetDateTime.parse("2026-06-28T10:15:30Z"), body["timestamp"])
        assertEquals(400, body["status"])
        assertSame(cause, body["error"])
        assertEquals("Required field shortTitle is missing", body["message"])
        assertEquals("/api/tests", body["path"])
        assertEquals("shortTitle", body["missingField"])
    }

    @Test
    fun `ordinary response status error omits missing field`() {
        val response = handler.handleResponseStatusException(
            ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"),
            request,
        )
        val body = response.body.orEmpty()

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, body["status"])
        assertEquals(null, body["error"])
        assertEquals("Test not found", body["message"])
        assertFalse(body.containsKey("missingField"))
    }
}
