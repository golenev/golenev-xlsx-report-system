package org.golenev.restapi.endpoints

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TestBatchRequest(
    val items: List<TestUpsertItem>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TestUpsertItem(
    val testId: String?,
    val category: String?,
    val shortTitle: String?,
    val issueLink: String? = null,
    val readyDate: String = LocalDate.now().toString(),
    val generalStatus: String? = "Готово",
    val priority: String? = "Medium",
    val scenario: ScenarioRequest?,
    val notes: String? = null,
    val runStatus: String? = null,
    val runDate: String? = null,
    val regressionStatus: String? = null,
)

data class TestReportResponse(
    val items: List<TestReportItemDto>,
    val columnConfig: Map<String, Int>,
    val translations: Map<String, String> = emptyMap(),
)

data class TestReportItemDto(
    val testId: String,
    val category: String?,
    val shortTitle: String?,
    val issueLink: String?,
    val readyDate: LocalDate?,
    val generalStatus: String?,
    val priority: String?,
    val scenario: ScenarioRequest?,
    val notes: String?,
    val updatedAt: String?,
    val runStatus: String? = null
)

@JsonDeserialize(using = ScenarioRequestDeserializer::class)
data class ScenarioRequest(
    val steps: List<ScenarioStepRequest>,
)

data class ScenarioStepRequest(
    val number: Int? = null,
    val text: String,
    val attachments: List<ScenarioAttachmentRequest>,
    val subSteps: List<ScenarioStepRequest> = emptyList(),
    val durationMs: Long? = null,
    val parameters: List<ScenarioParameterRequest> = emptyList(),
)

data class ScenarioAttachmentRequest(
    val name: String,
    val mediaType: String? = null,
    val content: String,
    val source: String? = null,
    val sizeBytes: Long? = null,
)

data class ScenarioParameterRequest(val name: String, val value: String)

class ScenarioRequestDeserializer : JsonDeserializer<ScenarioRequest>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ScenarioRequest {
        val codec = parser.codec
        val node = codec.readTree<JsonNode>(parser)
        val scenarioNode = if (node.isTextual) {
            codec.readTree<JsonNode>(codec.factory.createParser(node.asText()))
        } else {
            node
        }

        fun parseSteps(stepsNode: JsonNode): List<ScenarioStepRequest> = stepsNode
            .takeIf { it.isArray }
            ?.map { stepNode ->
                ScenarioStepRequest(
                    number = stepNode.path("number").takeIf { it.isNumber }?.asInt(),
                    text = stepNode.path("text").asText(),
                    attachments = stepNode.path("attachments")
                        .takeIf { it.isArray }
                        ?.map { attachmentNode ->
                            ScenarioAttachmentRequest(
                                name = attachmentNode.path("name").takeIf { !it.isMissingNode }?.asText()
                                    ?: attachmentNode.path("type").asText("Attachment"),
                                mediaType = attachmentNode.path("mediaType").takeIf { !it.isMissingNode && !it.isNull }?.asText(),
                                content = attachmentNode.path("content").asText(),
                                source = attachmentNode.path("source").takeIf { !it.isMissingNode && !it.isNull }?.asText(),
                                sizeBytes = attachmentNode.path("sizeBytes").takeIf { it.isNumber }?.asLong(),
                            )
                        }
                        .orEmpty(),
                    subSteps = parseSteps(stepNode.path("subSteps")),
                    durationMs = stepNode.path("durationMs").takeIf { it.isNumber }?.asLong(),
                    parameters = stepNode.path("parameters").takeIf { it.isArray }?.map { parameterNode ->
                        ScenarioParameterRequest(parameterNode.path("name").asText(), parameterNode.path("value").asText())
                    }.orEmpty(),
                )
            }
            .orEmpty()

        val steps = parseSteps(scenarioNode.path("steps"))

        return ScenarioRequest(steps = steps)
    }
}

data class ErrorResponse(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: Any? = null,
    val message: String? = null,
    val path: String? = null,
    val missingField: String? = null,
    val items: List<TestUpsertItem>? = null,
)

enum class GeneralTestStatus(val value: String) {
    QUEUE("Очередь"),
    IN_PROGRESS("В работе"),
    DONE("Готово"),
    BACKLOG("Бэклог"),
    MANUAL_ONLY("Только ручное"),
    OUTDATED("Неактуально"),
    FRONT("Фронт"),
}
