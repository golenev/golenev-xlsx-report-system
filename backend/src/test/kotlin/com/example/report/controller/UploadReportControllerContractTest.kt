package com.example.report.controller

import com.example.report.repository.TestReportRepository
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UploadReportControllerContractTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testReportRepository: TestReportRepository

    @BeforeEach
    fun cleanDb() {
        testReportRepository.deleteAll()
    }

    /**
     * Контрактный happy path проверяет эквивалентный класс валидной multipart-загрузки Allure: один result.json
     * с обязательными name, status, AS_ID, suite и шагами должен превращаться в строковый сценарий и сохраняться через batch API.
     */
    @Test
    fun `upload report accepts valid allure result and persists parsed test case`() {
        mockMvc.perform(
            multipart("/uploadReport")
                .file(allureResultFile("files", "allure-results/100-result.json", "100", "Checkout smoke", "Payments", "passed"))
                .param("paths", "allure-results/100-result.json")
        ).andExpect(status().isOk)

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].testId", equalTo("100")))
            .andExpect(jsonPath("$.items[0].category", equalTo("Payments")))
            .andExpect(jsonPath("$.items[0].shortTitle", equalTo("Checkout smoke")))
            .andExpect(jsonPath("$.items[0].scenario.steps[0].text", containsString("Open checkout")))
    }

    /**
     * Граничный позитивный кейс для параметризованных Allure-тестов: два result.json с одинаковым AS_ID должны
     * детерминированно получить суффиксы -1 и -2, чтобы не перетереть друг друга при сохранении.
     */
    @Test
    fun `upload report assigns deterministic suffixes for duplicate allure ids`() {
        mockMvc.perform(
            multipart("/uploadReport")
                .file(allureResultFile("files", "b-result.json", "777", "Second parameter", "API", "failed"))
                .file(allureResultFile("files", "a-result.json", "777", "First parameter", "API", "passed"))
                .param("paths", "b-result.json", "a-result.json")
        ).andExpect(status().isOk)

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].testId", equalTo("777-1")))
            .andExpect(jsonPath("$.items[0].shortTitle", equalTo("First parameter")))
            .andExpect(jsonPath("$.items[1].testId", equalTo("777-2")))
            .andExpect(jsonPath("$.items[1].shortTitle", equalTo("Second parameter")))
    }

    /**
     * Edge case проверяет связь result.json с отдельным вложением по source: если клиент передал paths,
     * контроллер должен использовать их вместо имён multipart-файлов и добавить очищенное HTML-вложение в сценарий.
     */
    @Test
    fun `upload report uses paths to resolve html attachment and sanitizes content`() {
        val result = allureResultFile(
            partName = "files",
            originalFilename = "random-upload-name.json",
            id = "201",
            name = "Attachment parsing",
            suite = "UI",
            status = "passed",
            attachmentSource = "attachments/body.html",
        )
        val attachment = MockMultipartFile(
            "files",
            "another-random-name.bin",
            "text/html",
            "<html><body><script>alert('x')</script><div>Expected&nbsp;body<br/>line two</div></body></html>".toByteArray(),
        )

        mockMvc.perform(
            multipart("/uploadReport")
                .file(result)
                .file(attachment)
                .param("paths", "results/201-result.json", "attachments/body.html")
        ).andExpect(status().isOk)

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tests"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].scenario.steps[*].text", hasItem(containsString("Expected body"))))
            .andExpect(jsonPath("$.items[0].scenario.steps[*].text", hasItem(equalTo("line two"))))
    }

    /**
     * Негативный класс невалидной загрузки: multipart содержит JSON-файл, но он не похож на Allure test case,
     * поэтому контроллер должен вернуть 400 и не создавать записи в отчёте.
     */
    @Test
    fun `upload report rejects multipart without allure test json`() {
        val metadata = MockMultipartFile(
            "files",
            "executor.json",
            "application/json",
            "{\"name\":\"executor\",\"type\":\"jenkins\"}".toByteArray(),
        )

        mockMvc.perform(multipart("/uploadReport").file(metadata))
            .andExpect(status().isBadRequest)

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()", equalTo(0)))
    }

    /**
     * Негативный граничный кейс обязательного AS_ID: минимально распознанный Allure result.json без label AS_ID
     * должен завершаться 400, иначе в отчёте появился бы тест без стабильного внешнего идентификатора.
     */
    @Test
    fun `upload report rejects allure result without as id label`() {
        val withoutId = MockMultipartFile(
            "files",
            "without-id-result.json",
            "application/json",
            """
                {
                  "name": "No id",
                  "status": "passed",
                  "labels": [{"name": "suite", "value": "API"}],
                  "steps": [{"name": "Do action"}]
                }
            """.trimIndent().toByteArray(),
        )

        mockMvc.perform(multipart("/uploadReport").file(withoutId))
            .andExpect(status().isBadRequest)
    }

    private fun allureResultFile(
        partName: String,
        originalFilename: String,
        id: String,
        name: String,
        suite: String,
        status: String,
        attachmentSource: String? = null,
    ): MockMultipartFile {
        val attachmentBlock = attachmentSource?.let { source ->
            """,
                    "attachments": [{"name": "response", "source": "$source", "type": "text/html"}]
            """.trimIndent()
        }.orEmpty()
        val content = """
            {
              "name": "$name",
              "status": "$status",
              "labels": [
                {"name": "AS_ID", "value": "$id"},
                {"name": "suite", "value": "$suite"}
              ],
              "steps": [
                {
                  "name": "Open checkout"$attachmentBlock
                },
                {"name": "Submit order"}
              ]
            }
        """.trimIndent()
        return MockMultipartFile(partName, originalFilename, "application/json", content.toByteArray())
    }
}
