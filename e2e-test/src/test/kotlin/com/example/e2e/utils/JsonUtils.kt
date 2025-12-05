package com.example.e2e.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JsonUtils {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val globalMapper = objectMapper

    fun <T> parse(content: String, clazz: Class<T>): T = objectMapper.readValue(content, clazz)

    fun toJson(value: Any): String = objectMapper.writeValueAsString(value)
}
