package helpers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.web.util.HtmlUtils

private const val MAX_ATTACHMENT_CHARS = 50_000
private val logger = LoggerFactory.getLogger(AllureReportParser::class.java)

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
    val scenario: String, // сценарий в том же виде, как раньше печатался
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
    val scenario: String, // блок "**Сценарий**: ..." со всеми шагами
    val category: String, // suite = имя категории/класса
    val runStatus: String?,
)

// ----------------- Вспомогательные функции -----------------

private fun baseName(path: String): String =
    path.substringAfterLast('/')
        .substringAfterLast('\\')

private fun isTestCaseJson(upload: AllureUpload, mapper: ObjectMapper): Boolean {
    if (!upload.path.lowercase().endsWith(".json")) return false

    val root: JsonNode = try {
        mapper.readTree(upload.content)
    } catch (_: Exception) {
        return false
    }

    val hasName = root.get("name")?.isTextual == true
    val hasStatus = root.get("status")?.isTextual == true
    val hasSteps = root.get("steps")?.isArray == true
    val hasStage = root.get("testStage")?.isObject == true
    val hasStageSteps = root.get("testStage")?.get("steps")?.isArray == true

    return hasName && (hasStatus || hasStage || hasSteps || hasStageSteps)
}

private fun isHtmlAttachment(source: String?, type: String?): Boolean {
    val lowerSource = source?.lowercase() ?: ""
    return lowerSource.endsWith(".html") || (type?.lowercase()?.contains("html") == true)
}

private fun isTextAttachment(source: String?, type: String?): Boolean {
    val lowerSource = source?.lowercase() ?: ""
    val lowerType = type?.lowercase()
    val textExtensions = listOf(".html", ".txt", ".log", ".json", ".xml")
    return (lowerType?.startsWith("text/") == true) || textExtensions.any { lowerSource.endsWith(it) }
}

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

private fun maskSecrets(text: String): String {
    val secretHeaders = listOf("Authorization", "Cookie", "X-Auth-Token")
    var result = text
    secretHeaders.forEach { header ->
        val regex = Regex("(?im)^(" + Regex.escape(header) + "):\\s*.*$")
        result = result.replace(regex, "$1: ****")
    }
    return result
}

private fun formatAttachmentContent(attachment: Attachment, upload: AllureUpload): String {
    val sourceName = attachment.source ?: upload.path
    val type = attachment.type
    val isHtml = isHtmlAttachment(sourceName, type)
    val isText = isTextAttachment(sourceName, type)

    if (!isText) {
        val safeType = type ?: "unknown"
        val size = attachment.size ?: upload.content.size.toLong()
        return "[binary attachment: $safeType; file=$sourceName; size=$size]"
    }

    val rawText = upload.content.toString(Charsets.UTF_8)
    val normalized = if (isHtml) sanitizeHtmlAttachment(rawText) else rawText
    val masked = maskSecrets(normalized)
    return if (masked.length > MAX_ATTACHMENT_CHARS) {
        masked.take(MAX_ATTACHMENT_CHARS) + "...TRUNCATED..."
    } else {
        masked
    }
}

// Парсинг одного JSON в RawTestCase
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

    fun processAttachments(step: Step, level: Int): List<String> {
        if (!attachmentsEnabled) return emptyList()

        val files = step.attachments ?: return emptyList()
        val indentPrefix = " ".repeat((level + 1) * 2)
        val contentIndent = " ".repeat((level + 2) * 2)

        return files.flatMap { attachment ->
            val sourceName = attachment.source ?: "unknown"
            val title = attachment.name ?: "Attachment"
            val header = "$indentPrefixAttachment: $title ($sourceName)"
            val upload = filesByName[baseName(sourceName)]
            val content = upload?.let { formatAttachmentContent(attachment, it) }
                ?: "[Attachment missing] $title -> $sourceName"

            val contentLines = content.lines().map { line -> "$contentIndent$line" }
            listOf(header) + contentLines
        }
    }

    fun processSteps(steps: List<Step>?, indent: Int = 0): List<String> {
        val seenSteps = mutableSetOf<String>()
        val lines = mutableListOf<String>()

        fun traverse(steps: List<Step>?, level: Int, numberingPrefix: List<Int>) {
            steps?.forEachIndexed { index, step ->
                val key = "${step.name}:${step.parameters?.joinToString { it.value } ?: ""}"
                if (key in seenSteps) return@forEachIndexed
                seenSteps.add(key)

                val numbering = (numberingPrefix + (index + 1)).joinToString(".")
                val prefix = " ".repeat(level * 2) + "• $numbering. ${step.name}"

                val withParams = buildString {
                    step.parameters?.takeIf { it.isNotEmpty() }?.let { params ->
                        appendLine()
                        appendLine("```")
                        params.forEach { param ->
                            appendLine("${param.name}=${param.value}")
                        }
                        append("```")
                    }
                }

                lines.add(prefix + withParams)
                lines.addAll(processAttachments(step, level))
                traverse(step.steps, level + 1, numberingPrefix + (index + 1))
            }
        }

        traverse(steps, indent, emptyList())
        return lines
    }

    fun renderBlock(title: String, steps: List<Step>?): String {
        val lines = processSteps(steps)
        return if (lines.isNotEmpty()) {
            buildString {
                appendLine("$title:")
                lines.forEach { appendLine(it) }
                appendLine()
            }
        } else ""
    }

    // Название теста — как раньше, только без префикса "**Название теста:**"
    val testName = report.name
        ?.replace(Regex("\\s+"), " ")
        ?: "Без названия"

    // Сценарий — тот самый блок "**Сценарий**", который раньше печатался
    val scenarioSteps = report.testStage?.steps ?: report.steps
    val scenarioBlock = renderBlock("**Сценарий**", scenarioSteps)
        .ifBlank { "Шаги не найдены" }

    // ID из лейбла AS_ID
    val baseId = report.labels
        ?.firstOrNull { it.name == "AS_ID" }
        ?.value

    // Категория из лейбла suite (DisplayName над классом)
    val category = report.labels
        ?.firstOrNull { it.name == "suite" }
        ?.value
        ?: "Без категории"

    val normalizedStatus = report.status
        ?.trim()
        ?.lowercase()
        ?.takeIf { it == "passed" || it == "failed" }

    return RawTestCase(
        baseId = baseId,
        name = testName,
        scenario = scenarioBlock,
        category = category,
        runStatus = normalizedStatus,
    )
}

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
 * Парсит JSON-файлы Allure, переданные в байтах, и возвращает список TestCaseModel.
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
