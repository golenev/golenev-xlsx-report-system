package com.example.report.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.DateTimeException
import java.time.ZoneId
import java.util.TimeZone

class TimeConfigUnitTest {

    @Test
    fun `configured zone controls application clock and process default time zone`() {
        val original = TimeZone.getDefault()
        try {
            val config = TimeConfig("Europe/Moscow")

            config.setDefaultTimeZone()

            assertEquals(ZoneId.of("Europe/Moscow"), config.applicationClock().zone)
            assertEquals("Europe/Moscow", TimeZone.getDefault().id)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `invalid configured zone fails when time configuration is initialized`() {
        val config = TimeConfig("Mars/Olympus")

        assertThrows(DateTimeException::class.java) { config.applicationClock() }
    }
}
