package com.example.report.contract

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class RegressionApiContractTest : ContractTestSupport() {

    @Test
    fun `current regression is idle when no run exists`() {
        mockMvc.get("/api/regressions/current")
            .andExpect {
                status { isOk() }
                jsonPath("$.status", equalTo("IDLE"))
                jsonPath("$.regressionDate", equalTo("2026-06-28"))
                jsonPath("$.results", equalTo(emptyMap<String, String>()))
                jsonPath("$.releaseName") { doesNotExist() }
            }
    }

    @Test
    fun `start trims release name and exposes running state`() {
        mockMvc.post("/api/regressions/start") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"releaseName":"  release-1  "}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.status", equalTo("RUNNING"))
            jsonPath("$.regressionDate", equalTo("2026-06-28"))
            jsonPath("$.releaseName", equalTo("release-1"))
            jsonPath("$.results", equalTo(emptyMap<String, String>()))
        }

        mockMvc.get("/api/regressions/current")
            .andExpect {
                jsonPath("$.status", equalTo("RUNNING"))
                jsonPath("$.releaseName", equalTo("release-1"))
            }
    }

    @Test
    fun `start validates blank duplicate and simultaneous releases`() {
        mockMvc.post("/api/regressions/start") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"releaseName":"   "}"""
        }.andExpect {
            status { isBadRequest() }
        }

        startRegression("release-active")

        mockMvc.post("/api/regressions/start") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"releaseName":"release-other"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("Regression is already running for release release-active"))
        }

        mockMvc.post("/api/regressions/cancel")
            .andExpect { status { isOk() } }
        startRegression("release-history")
        createTestCase("HISTORY-1")
        stopRegression(mapOf("HISTORY-1" to "PASSED"))

        mockMvc.post("/api/regressions/start") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"releaseName":"release-history"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("Regression with release name release-history already exists"))
        }
    }

    @Test
    fun `stop rejects absent run empty results invalid statuses and missing test results`() {
        mockMvc.post("/api/regressions/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"results":{"UNKNOWN":"PASSED"}}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("No running regression to stop"))
        }

        startRegression("release-validation")
        createTestCase("VALIDATION-1")

        mockMvc.post("/api/regressions/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"results":{}}"""
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/regressions/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"results":{"VALIDATION-1":"BROKEN"}}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("Invalid regression status BROKEN for VALIDATION-1"))
        }

        mockMvc.post("/api/regressions/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"results":{"ANOTHER":"PASSED"}}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("Regression statuses are required for all test cases"))
        }
    }

    @Test
    fun `completed regression exposes release snapshot and downloadable workbook`() {
        createTestCase("10", shortTitle = "Second by numeric order")
        createTestCase("2", shortTitle = "First by numeric order")
        startRegression("release-completed")

        stopRegression(mapOf("10" to " failed ", "2" to "passed"))
            .andExpect {
                status { isOk() }
                jsonPath("$.status", equalTo("COMPLETED"))
                jsonPath("$.releaseName", equalTo("release-completed"))
                jsonPath("$.results", equalTo(emptyMap<String, String>()))
            }

        mockMvc.get("/api/regressions/current")
            .andExpect {
                jsonPath("$.status", equalTo("IDLE"))
            }

        val regressionId = regressionRepository.findByReleaseName("release-completed").orElseThrow().id
        mockMvc.get("/api/regressions/releases")
            .andExpect {
                status { isOk() }
                jsonPath("$", hasSize<Any>(1))
                jsonPath("$[0].id", equalTo(regressionId.toInt()))
                jsonPath("$[0].name", equalTo("release-completed"))
                jsonPath("$[0].status", equalTo("COMPLETED"))
            }
        mockMvc.get("/api/regressions/$regressionId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id", equalTo(regressionId.toInt()))
                jsonPath("$.name", equalTo("release-completed"))
                jsonPath("$.snapshot.status", equalTo("COMPLETED"))
                jsonPath("$.snapshot.tests", hasSize<Any>(2))
                jsonPath("$.snapshot.tests[0].testId", equalTo("2"))
                jsonPath("$.snapshot.tests[0].regressionStatus", equalTo("PASSED"))
                jsonPath("$.snapshot.tests[1].testId", equalTo("10"))
                jsonPath("$.snapshot.tests[1].regressionStatus", equalTo("FAILED"))
                jsonPath("$.snapshot.tests[0].scenario.steps[0].text", equalTo("step 2"))
            }

        val workbook = mockMvc.get("/api/regressions/$regressionId/snapshot.xlsx")
            .andExpect {
                status { isOk() }
                header { string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=regression-$regressionId.xlsx") }
                content { contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
            }
            .andReturn()
            .response
            .contentAsByteArray

        assertTrue(workbook.size > 100)
        assertEquals('P'.code.toByte(), workbook[0])
        assertEquals('K'.code.toByte(), workbook[1])
    }

    @Test
    fun `cancel deletes empty run but keeps run containing synchronized results`() {
        startRegression("release-empty")

        mockMvc.post("/api/regressions/cancel")
            .andExpect {
                status { isOk() }
                jsonPath("$.status", equalTo("IDLE"))
            }
        mockMvc.get("/api/regressions/releases")
            .andExpect {
                jsonPath("$", hasSize<Any>(0))
            }

        startRegression("release-with-results")
        mockMvc.post("/api/tests/batch?isRegressRunning=true") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "items": [{
                    "testId":"CANCEL-1",
                    "category":"API",
                    "shortTitle":"Cancel contract",
                    "scenario":{"steps":[{"number":1,"text":"step","attachments":[]}]},
                    "runStatus":"PASSED"
                  }]
                }
            """.trimIndent()
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/regressions/cancel")
            .andExpect {
                status { isOk() }
                jsonPath("$.status", equalTo("COMPLETED"))
                jsonPath("$.releaseName", equalTo("release-with-results"))
                jsonPath("$.results", equalTo(emptyMap<String, String>()))
            }
        mockMvc.get("/api/regressions/releases")
            .andExpect {
                jsonPath("$", hasSize<Any>(1))
                jsonPath("$[0].name", equalTo("release-with-results"))
                jsonPath("$[0].status", equalTo("COMPLETED"))
            }
    }

    @Test
    fun `unknown regression id returns not found for snapshot and workbook`() {
        mockMvc.get("/api/regressions/999999")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message", equalTo("Regression 999999 not found"))
            }
        mockMvc.get("/api/regressions/999999/snapshot.xlsx")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message", equalTo("Regression 999999 not found"))
            }
    }

    private fun startRegression(releaseName: String) {
        mockMvc.post("/api/regressions/start") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"releaseName":"$releaseName"}"""
        }.andExpect {
            status { isOk() }
        }
    }

    private fun stopRegression(results: Map<String, String>) =
        mockMvc.post("/api/regressions/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = results.entries.joinToString(
                prefix = "{\"results\":{",
                postfix = "}}",
            ) { (testId, status) -> "\"$testId\":\"$status\"" }
        }
}
