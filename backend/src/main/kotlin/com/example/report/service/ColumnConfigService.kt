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
    @Value("\${column-config.path}") private val configLocation: String
) : InitializingBean {

    private val mapper = jacksonObjectMapper()
    @Volatile
    private var cachedConfig: ColumnConfigProperties = ColumnConfigProperties(emptyMap())

    override fun afterPropertiesSet() {
        cachedConfig = loadConfig()
    }

    fun getConfig(): ColumnConfigProperties = cachedConfig

    fun reload() {
        cachedConfig = loadConfig()
    }

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
