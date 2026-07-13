package org.golenev.ui.allure

data class UiEventAttachment(
    val alias: String?,
    val locator: String,
    val eventType: UiEventType,
    val operation: String,
    val successCondition: String?,
    val because: String?,
    val status: String,
    val errorMessage: String?,
)

enum class UiEventType {
    CHECK,
    ACTION,
}
