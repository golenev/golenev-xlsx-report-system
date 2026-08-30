package com.example.report.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StatusModelsUnitTest {

    @Test
    fun `general status accepts only exact configured display values`() {
        assertEquals(
            listOf("Очередь", "В работе", "Готово", "Бэклог", "Только ручное", "Неактуально", "Фронт"),
            GeneralTestStatus.values().map { it.value },
        )
        assertEquals("Готово", GeneralTestStatus.requireValid("Готово"))
        assertNull(GeneralTestStatus.requireValid(null))
        assertThrows(IllegalArgumentException::class.java) { GeneralTestStatus.requireValid("готово") }
        assertThrows(IllegalArgumentException::class.java) { GeneralTestStatus.requireValid(" Готово ") }
    }

    @Test
    fun `priority lookup is case insensitive and returns canonical display value`() {
        assertEquals(
            listOf("Critical", "Blocker", "High", "Medium", "Low", "Trivial"),
            Priority.values().map { it.value },
        )
        assertEquals(Priority.HIGH, Priority.fromValue("hIgH"))
        assertEquals("High", Priority.requireValid("HIGH"))
        assertNull(Priority.requireValid(null))
        assertThrows(IllegalArgumentException::class.java) { Priority.requireValid("Urgent") }
        assertThrows(IllegalArgumentException::class.java) { Priority.requireValid(" High ") }
    }

    @Test
    fun `regression run status accepts enum names and wire values after trim`() {
        assertEquals("PASSED", RegressionRunStatus.requireValid(" passed "))
        assertEquals("FAILED", RegressionRunStatus.requireValid("FAILED"))
        assertEquals("SKIPPED", RegressionRunStatus.requireValid("Skipped"))
        assertNull(RegressionRunStatus.requireValid(null))
        assertNull(RegressionRunStatus.requireValid("   "))
        assertThrows(IllegalArgumentException::class.java) { RegressionRunStatus.requireValid("BLOCKED") }
    }
}
