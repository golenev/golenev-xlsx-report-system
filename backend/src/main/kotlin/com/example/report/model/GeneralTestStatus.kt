package com.example.report.model

enum class GeneralTestStatus(val value: String) {
    QUEUE("Очередь"),
    IN_PROGRESS("В работе"),
    DONE("Готово"),
    BACKLOG("Бэклог"),
    MANUAL_ONLY("Только ручное"),
    OUTDATED("Неактуально"),
    FRONT("Фронт");

    companion object {
        fun fromValue(value: String?): GeneralTestStatus? =
            values().firstOrNull { it.value == value }

        fun requireValid(value: String?): String? {
            if (value == null) return null
            return fromValue(value)?.value
                ?: throw IllegalArgumentException("Неправильно написанное слово статуса: $value")
        }
    }
}
