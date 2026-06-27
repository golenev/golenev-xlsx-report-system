package com.example.report.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId
import java.util.TimeZone

@Configuration
class TimeConfig(
    @Value("\${app.time-zone:Europe/Moscow}")
    private val timeZone: String,
) {
    private val zoneId: ZoneId by lazy { ZoneId.of(timeZone) }

    @PostConstruct
    fun setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
    }

    @Bean
    fun applicationClock(): Clock = Clock.system(zoneId)
}
