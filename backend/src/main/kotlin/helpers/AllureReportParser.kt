package helpers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.example.report.dto.ScenarioAttachmentRequest
import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import com.example.report.dto.ScenarioParameterRequest
import org.slf4j.LoggerFactory
import org.springframework.web.util.HtmlUtils

private const val MAX_ATTACHMENT_CHARS = 50_000
private val logger = LoggerFactory.getLogger("AllureReportParser")

// ---- Модели для парсинга Allure JSON ----

@JsonIgnoreProperties(ignoreUnknown = true)
data class AllureReport(
    val name: String?,
    val testStage: TestStage?,
    val beforeStages: List<TestStage>?,
    val afterStages: List<TestStage>?,
    val labels: List<Label>?, // здесь лежат AS_ID, suite и прочие лейблы
    val status: String?,
    val steps: List<Step>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestStage(
    val name: String? = null,
    val steps: List<Step>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Step(
    val name: String,
    val parameters: List<Parameter>?,
    val steps: List<Step>?,
    val attachments: List<Attachment>?,
    val start: Long?,
    val stop: Long?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Parameter(
    val name: String,
    val value: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Attachment(
    val name: String?,
    val source: String?,
    val type: String?,
    val size: Long?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Label(
    val name: String?,
    val value: String?,
)

// ---- Модель, которую возвращаем наружу ----

data class TestCaseModel(
    val id: String, // 455 или 455-1, 455-2...
    val name: String, // название теста
    val scenario: ScenarioRequest, // структурированный сценарий с шагами, подшагами и вложениями
    val category: String, // значение @DisplayName на классе (Allure label "suite")
    val runStatus: String?,
)

data class AllureUpload(
    val path: String,
    val content: ByteArray,
)

// Внутренняя DTO: результат парсинга одного файла до окончательного назначения ID
private data class RawTestCase(
    val baseId: String?, // 455 из AS_ID, ещё без -1/-2
    val name: String, // чистое название теста
    val scenario: ScenarioRequest, // структурированный сценарий со всеми шагами
    val category: String, // suite = имя категории/класса
    val runStatus: String?,
)

// ----------------- Вспомогательные функции -----------------

/**
 * Отдаёт «голое» имя файла, чтобы сравнивать аттачи независимо от вложенных папок.
 */
private fun baseName(path: String): String =
    path.substringAfterLast('/')
        .substringAfterLast('\\')

/**
 * Проверяет, похоже ли содержимое на JSON с тестом Allure, чтобы не пытаться парсить всё подряд.
 */
private fun isTestCaseJson(upload: AllureUpload, mapper: ObjectMapper): Boolean {
    if (!upload.path.lowercase().endsWith(".json")) return false

    val root: JsonNode = try {
        mapper.readTree(upload.content)
    } catch (_: Exception) {
        return false
    }

    val hasName = root.get("name")?.isTextual ?: false
    val hasStatus = root.get("status")?.isTextual ?: false

    val stepsNode = root.get("steps")
    val hasSteps = stepsNode?.isArray ?: false

    val stageNode = root.get("testStage")
    val hasStage = stageNode?.isObject ?: false

    val stageStepsNode = stageNode?.get("steps")
    val hasStageSteps = stageStepsNode?.isArray ?: false

    return hasName && (hasStatus || hasStage || hasSteps || hasStageSteps)
}

/**
 * Определяет, можно ли трактовать вложение как HTML, чтобы почистить теги и не проглотить скрипты.
 */
private fun isHtmlAttachment(source: String?, type: String?): Boolean {
    val lowerSource = source?.lowercase() ?: ""
    val lowerType = type?.lowercase()
    val hasHtmlType = lowerType?.contains("html") ?: false
    return lowerSource.endsWith(".html") || hasHtmlType
}

/**
 * Проверяет, можно ли читать вложение как текст, чтобы не пытаться скармливать бинарник пользователю.
 */
private fun isTextAttachment(source: String?, type: String?): Boolean {
    val lowerSource = source?.lowercase() ?: ""
    val lowerType = type?.lowercase() ?: ""
    val textExtensions = listOf(".html", ".txt", ".log", ".json", ".xml")
    val isExplicitText = lowerType.startsWith("text/")
    val matchesExtension = textExtensions.any { lowerSource.endsWith(it) }
    return isExplicitText || matchesExtension
}

/**
 * Очищает HTML от скриптов и оборачивает теги в читаемый текст, чтобы сценарий выглядел как обычный лог.
 */
private fun sanitizeHtmlAttachment(raw: String): String {
    val withoutScripts = raw.replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
    val withLineBreaks = withoutScripts
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>"), "\n")
        .replace(Regex("(?i)</div>"), "\n")
        .replace(Regex("(?i)</pre>"), "\n")
    val withoutTags = withLineBreaks.replace(Regex("(?s)<[^>]+>"), "")
    return HtmlUtils.htmlUnescape(withoutTags).trim()
}

/**
 * Представляет содержимое вложения в текстовом виде, аккуратно обрабатывая пропавшие файлы и длину контента.
 */
private fun formatAttachmentContent(attachment: Attachment, upload: AllureUpload): String {
    val sourceName = attachment.source ?: upload.path
    val type = attachment.type
    val isHtml = isHtmlAttachment(sourceName, type)
    val isText = isTextAttachment(sourceName, type)

    if (!isText) {
        val safeType = type ?: "unknown"
        val size = attachment.size ?: upload.content.size.toLong()
        return "[binary attachment: $safeType; size=$size]"
    }

    val rawText = upload.content.toString(Charsets.UTF_8)
    val normalized = if (isHtml) sanitizeHtmlAttachment(rawText) else rawText
    return if (normalized.length > MAX_ATTACHMENT_CHARS) {
        normalized.take(MAX_ATTACHMENT_CHARS) + "...TRUNCATED..."
    } else {
        normalized
    }
}

/**
 * Разбирает один JSON-файл Allure и превращает его в промежуточную модель теста с учётом вложений.
 */
private fun extractRawTestCase(
    jsonString: String,
    fileName: String,
    filesByName: Map<String, AllureUpload>,
    attachmentsEnabled: Boolean,
): RawTestCase {
    val mapper = jacksonObjectMapper()
    val report = try {
        mapper.readValue<AllureReport>(jsonString)
    } catch (e: Exception) {
        throw IllegalStateException("Ошибка при парсинге JSON из файла $fileName: ${e.message}", e)
    }

    /**
     * Достаёт вложения шага и превращает их в структурированные attachment DTO.
     */
    fun processAttachments(step: Step): List<ScenarioAttachmentRequest> {
        val files = step.attachments.orEmpty()
        if (files.isEmpty()) return emptyList()

        return files.map { attachment ->
            val sourceName = attachment.source ?: "unknown"
            val title = attachment.name ?: "Attachment"
            val upload = filesByName[baseName(sourceName)]
            val content = upload?.let { formatAttachmentContent(attachment, it) }
                ?: "[Attachment file is missing]"
            ScenarioAttachmentRequest(
                name = title,
                mediaType = attachment.type,
                content = content,
                source = sourceName,
                sizeBytes = upload?.content?.size?.toLong() ?: attachment.size,
            )
        }
    }

    /**
     * Обходит сырые Allure steps без промежуточного текста, чтобы не терять parent -> sub-step связь.
     */
    fun processSteps(steps: List<Step>?): List<ScenarioStepRequest> {
        fun traverse(steps: List<Step>?): List<ScenarioStepRequest> {
            if (steps == null) return emptyList()

            return steps.mapIndexed { index, step ->
                ScenarioStepRequest(
                    number = index + 1,
                    text = step.name,
                    attachments = processAttachments(step),
                    subSteps = traverse(step.steps),
                    durationMs = if (step.start != null && step.stop != null && step.stop >= step.start) step.stop - step.start else null,
                    parameters = step.parameters.orEmpty().map { parameter ->
                        ScenarioParameterRequest(name = parameter.name, value = parameter.value)
                    },
                )
            }
        }

        return traverse(steps)
    }

    // Название теста — как раньше, только без префикса "**Название теста:**"
    val testName = report.name
        ?.replace(Regex("\\s+"), " ")
        ?: "Без названия" // name?.replace -> дефолт, если имя пустое

    // Сценарий
    val scenarioSteps = report.testStage?.steps ?: report.steps // testStage?.steps -> fallback в steps
    val scenario = ScenarioRequest(steps = processSteps(scenarioSteps))

    // ID из лейбла AS_ID
    val baseId = report.labels
        ?.firstOrNull { it.name == "AS_ID" }
        ?.value // labels?.firstOrNull -> ?.value, используется для постфиксов параметризованных тестов

    // Категория из лейбла suite (DisplayName над классом)
    val category = report.labels
        ?.firstOrNull { it.name == "suite" }
        ?.value
        ?: "Без категории" // labels?.firstOrNull -> ?.value -> дефолтное значение

    val normalizedStatus = report.status
        ?.trim()
        ?.lowercase()
        ?.takeIf { status -> status == "passed" || status == "failed" || status == "skipped" }

    return RawTestCase(
        baseId = baseId,
        name = testName,
        scenario = scenario,
        category = category,
        runStatus = normalizedStatus,
    )
}

/**
 * Упорядочивает сырые кейсы, проверяет наличие AS_ID и проставляет хвосты -1/-2 при повторах.
 */
private fun buildTestCaseModels(rawCases: List<Pair<String, RawTestCase>>): List<TestCaseModel> {
    val withoutId = rawCases.filter { it.second.baseId == null }
    if (withoutId.isNotEmpty()) {
        val fileNames = withoutId.joinToString { it.first.substringAfterLast('/') }
        throw IllegalStateException("Для некоторых тестов не найден AS_ID (Label name='AS_ID'): $fileNames")
    }

    val groupedByBaseId: Map<String, List<Pair<String, RawTestCase>>> =
        rawCases.groupBy { it.second.baseId!! }

    val result = mutableListOf<TestCaseModel>()

    groupedByBaseId.forEach { (baseId, group) ->
        if (group.size == 1) {
            val raw = group.first().second
            result.add(
                TestCaseModel(
                    id = baseId,
                    name = raw.name,
                    scenario = raw.scenario,
                    category = raw.category,
                    runStatus = raw.runStatus,
                )
            )
        } else {
            val sortedGroup = group.sortedBy { it.first }
            sortedGroup.forEachIndexed { index, (_, raw) ->
                val runNumber = index + 1
                val id = "$baseId-$runNumber"
                result.add(
                    TestCaseModel(
                        id = id,
                        name = raw.name,
                        scenario = raw.scenario,
                        category = raw.category,
                        runStatus = raw.runStatus,
                    )
                )
            }
        }
    }
    return result
}

/**
 * Принимает набор загруженных файлов allure-results, вытаскивает из них тесты и возвращает готовые сценарии.
 */
fun parseAllureReportsFromUploads(uploads: List<AllureUpload>): List<TestCaseModel> {
    require(uploads.isNotEmpty()) {
        "Файлы отчёта не найдены в загрузке"
    }

    val mapper = jacksonObjectMapper()
    val classification = uploads.associateWith { isTestCaseJson(it, mapper) }
    val testCaseUploads = classification.filterValues { it }.keys
    val attachmentsEnabled = uploads.any { upload ->
        val fileName = baseName(upload.path).lowercase()
        !fileName.endsWith(".json") || fileName.contains("attachment")
    }

    if (!attachmentsEnabled) {
        logger.warn("Attachments files not provided; scenario will not include request/response")
    }

    if (testCaseUploads.isEmpty()) {
        throw IllegalStateException("JSON-файлы тестов не найдены в загрузке")
    }

    val filesByName = uploads.associateBy { baseName(it.path) }
    val rawCases: MutableList<Pair<String, RawTestCase>> = mutableListOf()

    testCaseUploads.forEach { upload ->
        val content = upload.content.toString(Charsets.UTF_8)
        val raw = extractRawTestCase(content, upload.path, filesByName, attachmentsEnabled)
        rawCases.add(upload.path to raw)
    }

    return buildTestCaseModels(rawCases)
}
