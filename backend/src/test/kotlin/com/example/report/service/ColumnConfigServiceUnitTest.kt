package com.example.report.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.DefaultResourceLoader
import java.nio.file.Path
import kotlin.io.path.writeText

class ColumnConfigServiceUnitTest {

    @TempDir
    lateinit var tempDir: Path

    /**
     * Позитивный unit-тест проверяет загрузку column-config из file-system и reload: если кэш не обновится,
     * сообщение покажет старые и ожидаемые width/translations.
     */
    @Test
    fun `loads file config on init and refreshes cached config on reload`() {
        val config = tempDir.resolve("column-config.json")
        config.writeText("""{"columns":{"testId":100},"translations":{"testId":"ID"}}""")
        val service = ColumnConfigService(DefaultResourceLoader(), config.toString())

        service.afterPropertiesSet()
        assertEquals(100, service.getConfig().columns["testId"], "Первичная загрузка file config вернула неверную ширину testId")
        assertEquals("ID", service.getConfig().translations["testId"], "Первичная загрузка file config вернула неверный перевод testId")

        config.writeText("""{"columns":{"testId":240,"notes":80},"translations":{"notes":"Заметки"}}""")
        service.reload()

        assertEquals(mapOf("testId" to 240, "notes" to 80), service.getConfig().columns, "reload должен заменить cached columns целиком")
        assertEquals(mapOf("notes" to "Заметки"), service.getConfig().translations, "reload должен заменить cached translations целиком")
    }

    /**
     * Негативный edge-тест проверяет диагностику повреждённого JSON: сервис должен падать на init,
     * чтобы приложение не работало с частично прочитанной конфигурацией колонок.
     */
    @Test
    fun `init fails fast for malformed column config json`() {
        val config = tempDir.resolve("broken-column-config.json")
        config.writeText("""{"columns":{"testId":"wide"}}""")
        val service = ColumnConfigService(DefaultResourceLoader(), config.toString())

        assertThrows(Exception::class.java, { service.afterPropertiesSet() }, "Malformed column-config должен приводить к ошибке инициализации")
    }
}
