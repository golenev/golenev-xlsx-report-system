package com.example.report.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal val fixedClock: Clock = Clock.fixed(Instant.parse("2026-06-28T10:15:30Z"), ZoneId.of("UTC"))

internal fun assertBadRequest(messagePart: String, action: () -> Unit): ResponseStatusException {
    val exception = org.junit.jupiter.api.assertThrows<ResponseStatusException>(
        "Ожидали HTTP 400 BAD_REQUEST с фрагментом '$messagePart', но операция завершилась без ошибки",
        action,
    )
    assertEquals(
        HttpStatus.BAD_REQUEST,
        exception.statusCode,
        "Неверный HTTP-статус для негативного кейса. reason='${exception.reason}'",
    )
    assertTrue(
        exception.reason?.contains(messagePart) == true,
        "Диагностика ошибки не содержит ожидаемый фрагмент. expectedPart='$messagePart', actualReason='${exception.reason}'",
    )
    return exception
}

internal fun assertNotFound(messagePart: String, action: () -> Unit): ResponseStatusException {
    val exception = org.junit.jupiter.api.assertThrows<ResponseStatusException>(
        "Ожидали HTTP 404 NOT_FOUND с фрагментом '$messagePart', но операция завершилась без ошибки",
        action,
    )
    assertEquals(
        HttpStatus.NOT_FOUND,
        exception.statusCode,
        "Неверный HTTP-статус для негативного кейса. reason='${exception.reason}'",
    )
    assertTrue(
        exception.reason?.contains(messagePart) == true,
        "Диагностика ошибки не содержит ожидаемый фрагмент. expectedPart='$messagePart', actualReason='${exception.reason}'",
    )
    return exception
}

internal fun assertCellEquals(expected: String, actual: String, address: String) {
    assertEquals(expected, actual, "Неожиданное значение Excel-ячейки $address")
    assertNotNull(actual, "Excel-ячейка $address не должна быть null")
}
