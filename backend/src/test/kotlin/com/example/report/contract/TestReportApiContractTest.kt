package com.example.report.contract

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.io.ByteArrayInputStream

class TestReportApiContractTest : ContractTestSupport() {

    @Test
    fun `empty report returns stable top level object with columns and translations`() {
        mockMvc.get("/api/tests")
            .andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.items", hasSize<Any>(0))
                jsonPath("$.columnConfig.testId", equalTo(180))
                jsonPath("$.columnConfig.scenario", equalTo(300))
                jsonPath("$.translations['Detailed Scenario']", equalTo("Детальный сценарий"))
                jsonPath("$.translations['Regress Run']", equalTo("Режим запуска регресса"))
            }
    }

    @Test
    fun `post accepts complete structured scenario and get returns every nested field as object`() {
        mockMvc.post("/api/tests?forceUpdate=true") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "testId": "CONTRACT-STRUCTURED",
                  "category": "E2E_FOR_AUTOTEST",
                  "shortTitle": "Structured scenario contract",
                  "issueLink": "https://youtrack.test/issue/CONTRACT-STRUCTURED",
                  "readyDate": "2026-06-25",
                  "generalStatus": "Готово",
                  "priority": "Medium",
                  "scenario": {
                    "steps": [{
                      "number": 1,
                      "text": "Формируем batch",
                      "attachments": [{
                        "type": "text",
                        "mediaType": "text/plain",
                        "content": "Batch содержит 10 элементов.\nКаждый testId уникален.",
                        "source": "request.txt",
                        "sizeBytes": 54
                      }],
                      "durationMs": 62001,
                      "parameters": [],
                      "subSteps": [{
                        "number": 1,
                        "text": "child",
                        "durationMs": 24,
                        "parameters": [{"name": "expectedDate", "value": "2026-07-16"}],
                        "attachments": [],
                        "subSteps": [{"number": 1, "text": "leaf", "attachments": [], "subSteps": []}]
                      }]
                    }]
                  },
                  "notes": ""
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            content { string("") }
        }

        mockMvc.get("/api/tests")
            .andExpect {
                status { isOk() }
                jsonPath("$.items", hasSize<Any>(1))
                jsonPath("$.items[0].testId", equalTo("CONTRACT-STRUCTURED"))
                jsonPath("$.items[0].readyDate", equalTo("2026-06-25"))
                jsonPath("$.items[0].scenario.steps[0].number", equalTo(1))
                jsonPath("$.items[0].scenario.steps[0].attachments[0].name", equalTo("text"))
                jsonPath("$.items[0].scenario.steps[0].attachments[0].content", containsString("Каждый testId уникален"))
                jsonPath("$.items[0].scenario.steps[0].attachments[0].source", equalTo("request.txt"))
                jsonPath("$.items[0].scenario.steps[0].attachments[0].sizeBytes", equalTo(54))
                jsonPath("$.items[0].scenario.steps[0].durationMs", equalTo(62001))
                jsonPath("$.items[0].scenario.steps[0].subSteps[0].parameters[0].value", equalTo("2026-07-16"))
                jsonPath("$.items[0].scenario.steps[0].subSteps[0].subSteps[0].text", equalTo("leaf"))
            }
    }

    @Test
    fun `post without force update applies server defaults and ignores supplied manual fields`() {
        mockMvc.post("/api/tests") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "testId":"CONTRACT-DEFAULTS",
                  "category":"API",
                  "shortTitle":"Defaults",
                  "issueLink":"https://ignored/issue",
                  "readyDate":"2020-01-01",
                  "generalStatus":"invalid but ignored",
                  "priority":"Urgent",
                  "notes":"ignored",
                  "runStatus":"FAILED",
                  "scenario":{"steps":[{"number":1,"text":"step","attachments":[]}]}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/api/tests")
            .andExpect {
                status { isOk() }
                jsonPath("$.items[0].issueLink", equalTo("https://youtrackru/issue/"))
                jsonPath("$.items[0].readyDate", equalTo("2026-06-28"))
                jsonPath("$.items[0].generalStatus", equalTo("Готово"))
                jsonPath("$.items[0].priority", equalTo("Medium"))
                jsonPath("$.items[0].notes", equalTo(""))
                jsonPath("$.items[0].runStatus") { doesNotExist() }
                jsonPath("$.items[0].updatedAt", equalTo("2026-06-28T13:15:30+03:00"))
            }
    }

    @Test
    fun `post update without force changes required fields and preserves manual fields`() {
        createTestCase("CONTRACT-UPDATE")

        mockMvc.post("/api/tests") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "testId":"CONTRACT-UPDATE",
                  "category":"Updated API",
                  "shortTitle":"Updated title",
                  "issueLink":"https://ignored/updated",
                  "readyDate":"2030-01-01",
                  "generalStatus":"Очередь",
                  "priority":"High",
                  "notes":"ignored update",
                  "scenario":{"steps":[{"number":2,"text":"updated step","attachments":[]}]}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/api/tests")
            .andExpect {
                jsonPath("$.items[0].category", equalTo("Updated API"))
                jsonPath("$.items[0].shortTitle", equalTo("Updated title"))
                jsonPath("$.items[0].scenario.steps[0].number", equalTo(2))
                jsonPath("$.items[0].issueLink", equalTo("https://issue/CONTRACT-UPDATE"))
                jsonPath("$.items[0].readyDate", equalTo("2026-01-15"))
                jsonPath("$.items[0].generalStatus", equalTo("Готово"))
                jsonPath("$.items[0].priority", equalTo("Medium"))
                jsonPath("$.items[0].notes", equalTo("notes CONTRACT-UPDATE"))
            }
    }

    @Test
    fun `post returns structured missing field error body`() {
        mockMvc.post("/api/tests") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"testId":"   ","category":"API","shortTitle":"Missing id","scenario":{"steps":[{"number":1,"text":"step","attachments":[]}]}}
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.timestamp", equalTo("2026-06-28T13:15:30+03:00"))
            jsonPath("$.status", equalTo(400))
            jsonPath("$.message", equalTo("Required field testId is missing"))
            jsonPath("$.missingField", equalTo("testId"))
            jsonPath("$.path", equalTo("/api/tests"))
            jsonPath("$.error") { doesNotExist() }
        }
    }

    @Test
    fun `post rejects scenario string and step without attachments array`() {
        mockMvc.post("/api/tests") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"testId":"CONTRACT-STRING","category":"API","shortTitle":"String scenario","scenario":"{\"steps\":[]}"}
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/tests") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"testId":"CONTRACT-NO-ATTACHMENTS","category":"API","shortTitle":"No attachments","scenario":{"steps":[{"number":1,"text":"step"}]}}
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("Field scenario.steps.attachments must be an array"))
            jsonPath("$.path", equalTo("/api/tests"))
        }
    }

    @Test
    fun `delete removes existing test and missing test returns exact not found contract`() {
        createTestCase("CONTRACT-DELETE")

        mockMvc.delete("/api/tests/CONTRACT-DELETE")
            .andExpect {
                status { isOk() }
                content { string("") }
            }
        mockMvc.get("/api/tests")
            .andExpect {
                jsonPath("$.items", hasSize<Any>(0))
            }
        mockMvc.delete("/api/tests/CONTRACT-DELETE")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.status", equalTo(404))
                jsonPath("$.message", equalTo("Test with ID CONTRACT-DELETE not found"))
                jsonPath("$.path", equalTo("/api/tests/CONTRACT-DELETE"))
            }
    }

    @Test
    fun `excel export preserves oversized scenario in visually joined rows`() {
        val oversizedAttachment = "x".repeat(40_000)
        mockMvc.post("/api/tests?forceUpdate=true") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "testId":"CONTRACT-XLSX",
                  "category":"API",
                  "shortTitle":"Oversized export",
                  "scenario":{"steps":[{"number":1,"text":"step","attachments":[{"name":"log","content":"$oversizedAttachment"}]}]}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }

        val response = mockMvc.get("/api/tests/export/excel")
            .andExpect {
                status { isOk() }
                header { string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tests.xlsx") }
                content { contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
            }
            .andReturn()
            .response

        assertTrue(response.contentAsByteArray.size > 100, "XLSX response должен содержать workbook")
        assertEquals('P'.code.toByte(), response.contentAsByteArray[0])
        assertEquals('K'.code.toByte(), response.contentAsByteArray[1])

        XSSFWorkbook(ByteArrayInputStream(response.contentAsByteArray)).use { workbook ->
            val sheet = workbook.getSheet("Test Report")
            val scenario = (1..sheet.lastRowNum).joinToString("") { rowIndex ->
                sheet.getRow(rowIndex).getCell(7).stringCellValue
            }

            assertEquals("step\n   [log] $oversizedAttachment", scenario)
            assertTrue(
                sheet.mergedRegions.any { region ->
                    region.firstRow == 1 && region.lastRow == 2 && region.firstColumn == 0 && region.lastColumn == 0
                },
                "Колонки тест-кейса должны визуально объединяться по высоте частей сценария",
            )
        }
    }

    @Test
    fun `column config endpoint returns columns and translations object`() {
        mockMvc.get("/api/config/columns")
            .andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.columns.testId", equalTo(180))
                jsonPath("$.columns.regressionStatus", equalTo(180))
                jsonPath("$.translations['Cancel']", equalTo("Отменить"))
                jsonPath("$.translations['Would you run regress']", equalTo("Запускаем регресс?"))
            }
    }
}
