package com.example.report.service

import com.example.report.config.ColumnConfigProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service

@Service
class ColumnConfigService(
    private val resourceLoader: ResourceLoader,
    /**
     * Путь к JSON-конфигурации колонок из настройки `column-config.path`.
     * Аннотация `@Value` подставляет значение свойства Spring из application-конфигурации в параметр конструктора.
     */
    @Value("\${column-config.path}") private val configLocation: String
) : InitializingBean {

    private val mapper = jacksonObjectMapper()
    /**
     * Кэш последней загруженной конфигурации колонок.
     * Аннотация `@Volatile` делает обновления ссылки видимыми для других потоков без дополнительной синхронизации.
     */
    @Volatile
    private var cachedConfig: ColumnConfigProperties = ColumnConfigProperties(emptyMap())

    /**
     * Загружает конфигурацию колонок сразу после инициализации Spring-бина.
     */
    override fun afterPropertiesSet() {
        cachedConfig = loadConfig()
    }

    /**
     * Возвращает текущую кэшированную конфигурацию колонок.
     */
    fun getConfig(): ColumnConfigProperties = cachedConfig

    /**
     * Принудительно перечитывает конфигурацию колонок из настроенного ресурса и обновляет кэш.
     */
    fun reload() {
        cachedConfig = loadConfig()
    }

    /**
     * Читает JSON-конфигурацию колонок из classpath-ресурса или файла и преобразует её в объект настроек.
     */
    private fun loadConfig(): ColumnConfigProperties {
        val resource = if (configLocation.startsWith("classpath:")) {
            resourceLoader.getResource(configLocation)
        } else {
            resourceLoader.getResource("file:" + configLocation)
        }
        resource.inputStream.use { input ->
            return mapper.readValue(input)
        }
    }
}
