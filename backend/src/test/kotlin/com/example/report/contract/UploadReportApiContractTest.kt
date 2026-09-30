package com.example.report.contract

import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post

class UploadReportApiContractTest : ContractTestSupport() {

    @Test
    fun `upload requires multipart files parameter`() {
        mockMvc.multipart("/uploadReport")
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `upload rejects collection without allure test json`() {
        val file = MockMultipartFile(
            "files",
            "environment.json",
            MediaType.APPLICATION_JSON_VALUE,
            """{"environment":"test"}""".toByteArray(),
        )

        mockMvc.multipart("/uploadReport") {
            file(file)
            param("paths", "allure-results/environment.json")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("JSON-файлы тестов не найдены в загрузке"))
            jsonPath("$.path", equalTo("/uploadReport"))
        }
    }

    @Test
    fun `upload creates test from allure json and resolves text attachment`() {
        val report = allureResult(
            testId = "UPLOAD-1",
            name = "Uploaded contract",
            status = "passed",
            attachmentSource = "request.txt",
        )
        val resultFile = MockMultipartFile(
            "files",
            "result.json",
            MediaType.APPLICATION_JSON_VALUE,
            report.toByteArray(),
        )
        val attachmentFile = MockMultipartFile(
            "files",
            "request.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "request body from attachment".toByteArray(),
        )

        mockMvc.multipart("/uploadReport") {
            file(resultFile)
            file(attachmentFile)
            param("paths", "allure-results/result.json", "allure-results/request.txt")
        }.andExpect {
            status { isOk() }
            content { string("") }
        }

        mockMvc.get("/api/tests")
            .andExpect {
                jsonPath("$.items", hasSize<Any>(1))
                jsonPath("$.items[0].testId", equalTo("UPLOAD-1"))
                jsonPath("$.items[0].category", equalTo("API contract"))
                jsonPath("$.items[0].shortTitle", equalTo("Uploaded contract"))
                jsonPath("$.items[0].scenario.steps[0].text", equalTo("root step"))
                jsonPath("$.items[0].scenario.steps[0].attachments[0].name", equalTo("request"))
                jsonPath("$.items[0].scenario.steps[0].attachments[0].content", equalTo("request body from attachment"))
                jsonPath("$.items[0].scenario.steps[0].attachments[0].source", equalTo("request.txt"))
                jsonPath("$.items[0].scenario.steps[0].attachments[0].sizeBytes", equalTo(28))
                jsonPath("$.items[0].runStatus") { doesNotExist() }
            }
    }

    @Test
    fun `upload rejects allure result that references missing attachment file without changing report`() {
        createTestCase("UNCHANGED")
        val resultFile = MockMultipartFile(
            "files",
            "missing-attachment-result.json",
            MediaType.APPLICATION_JSON_VALUE,
            allureResult("UPLOAD-MISSING", "Incomplete Allure", "failed", "response.txt").toByteArray(),
        )

        mockMvc.multipart("/uploadReport") {
            file(resultFile)
            param("paths", "allure-results/missing-attachment-result.json")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", containsString("testId=UPLOAD-MISSING"))
            jsonPath("$.message", containsString("stepNumber=1"))
            jsonPath("$.message", containsString("missing-attachment-result.json"))
            jsonPath("$.message", containsString("response.txt"))
        }

        mockMvc.get("/api/tests").andExpect {
            jsonPath("$.items", hasSize<Any>(1))
            jsonPath("$.items[0].testId", equalTo("UNCHANGED"))
        }
    }

    @Test
    fun `upload keeps manual fields of existing test by default`() {
        createTestCase("UPLOAD-EXISTING")
        val resultFile = MockMultipartFile(
            "files",
            "existing-result.json",
            MediaType.APPLICATION_JSON_VALUE,
            allureResult("UPLOAD-EXISTING", "Updated from Allure", "passed").toByteArray(),
        )

        mockMvc.multipart("/uploadReport") {
            file(resultFile)
            param("paths", "allure-results/existing-result.json")
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/api/tests")
            .andExpect {
                jsonPath("$.items[0].shortTitle", equalTo("Updated from Allure"))
                jsonPath("$.items[0].issueLink", equalTo("https://issue/UPLOAD-EXISTING"))
                jsonPath("$.items[0].readyDate", equalTo("2026-01-15"))
                jsonPath("$.items[0].generalStatus", equalTo("Готово"))
                jsonPath("$.items[0].priority", equalTo("Medium"))
                jsonPath("$.items[0].notes", equalTo("notes UPLOAD-EXISTING"))
            }
    }

    @Test
    fun `regression upload requires active run and synchronizes normalized allure status`() {
        val resultFile = MockMultipartFile(
            "files",
            "regression-result.json",
            MediaType.APPLICATION_JSON_VALUE,
            allureResult("UPLOAD-RUN", "Regression upload", "failed").toByteArray(),
        )

        mockMvc.multipart("/uploadReport?isRegressRunning=true") {
            file(resultFile)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message", equalTo("регресс не запущен, сначала запустите регресс"))
        }

        mockMvc.post("/api/regressions/start") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"releaseName":"upload-release"}"""
        }.andExpect { status { isOk() } }

        mockMvc.multipart("/uploadReport?isRegressRunning=true") {
            file(resultFile)
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/api/tests")
            .andExpect {
                jsonPath("$.items[0].runStatus") { doesNotExist() }
            }
        mockMvc.get("/api/regressions/current")
            .andExpect {
                jsonPath("$.results.UPLOAD-RUN", equalTo("FAILED"))
            }
    }

    @Test
    fun `upload reports allure test without AS ID as bad request`() {
        val report = """
            {
              "name":"No AS ID",
              "status":"passed",
              "labels":[{"name":"suite","value":"API contract"}],
              "testStage":{"steps":[]}
            }
        """.trimIndent()
        val resultFile = MockMultipartFile(
            "files",
            "missing-id-result.json",
            MediaType.APPLICATION_JSON_VALUE,
            report.toByteArray(),
        )

        mockMvc.multipart("/uploadReport") {
            file(resultFile)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", containsString("Для некоторых тестов не найден AS_ID"))
        }
    }

    private fun allureResult(
        testId: String,
        name: String,
        status: String,
        attachmentSource: String? = null,
    ): String {
        val attachments = attachmentSource?.let {
            """[{"name":"request","source":"$it","type":"text/plain"}]"""
        } ?: "[]"
        return """
            {
              "name":"$name",
              "status":"$status",
              "labels":[
                {"name":"AS_ID","value":"$testId"},
                {"name":"suite","value":"API contract"}
              ],
              "testStage":{"steps":[{"name":"root step","attachments":$attachments}]}
            }
        """.trimIndent()
    }
}
