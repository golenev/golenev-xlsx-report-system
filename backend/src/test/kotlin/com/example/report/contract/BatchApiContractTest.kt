package com.example.report.contract

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class BatchApiContractTest : ContractTestSupport() {

    @Test
    fun `batch rejects empty items through bean validation`() {
        mockMvc.post("/api/tests/batch") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"items":[]}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `batch validates all items before persisting any row`() {
        mockMvc.post("/api/tests/batch") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "items": [
                    {"testId":"BATCH-VALID","category":"API","shortTitle":"Valid","scenario":{"steps":[{"number":1,"text":"step","attachments":[]}]}},
                    {"testId":"BATCH-INVALID","category":"API","scenario":{"steps":[{"number":1,"text":"step","attachments":[]}]}}
                  ]
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("Required field shortTitle is missing"))
            jsonPath("$.missingField", equalTo("shortTitle"))
        }

        mockMvc.get("/api/tests")
            .andExpect {
                jsonPath("$.items", hasSize<Any>(0))
            }
    }

    @Test
    fun `batch with regression flag rejects request when current regression is absent`() {
        mockMvc.post("/api/tests/batch?isRegressRunning=true") {
            contentType = MediaType.APPLICATION_JSON
            content = batchBody("BATCH-NO-REGRESSION", "PASSED")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.status", equalTo(404))
            jsonPath("$.message", equalTo("регресс не запущен, сначала запустите регресс"))
            jsonPath("$.path", equalTo("/api/tests/batch"))
        }
    }

    @Test
    fun `running batch accepts snake case status alias and synchronizes current regression`() {
        startRegression("batch-release")

        mockMvc.post("/api/tests/batch?isRegressRunning=true&forceUpdate=true") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "items": [
                    {
                      "testId":"BATCH-RUN-1",
                      "category":"API",
                      "shortTitle":"Passed",
                      "priority":"high",
                      "scenario":{"steps":[{"number":1,"text":"step 1","attachments":[]}]},
                      "run_status":"passed"
                    },
                    {
                      "testId":"BATCH-RUN-2",
                      "category":"UI",
                      "shortTitle":"Skipped",
                      "scenario":{"steps":[{"number":1,"text":"step 2","attachments":[]}]},
                      "runStatus":"SKIPPED"
                    }
                  ]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            content { string("") }
        }

        mockMvc.get("/api/tests")
            .andExpect {
                jsonPath("$.items", hasSize<Any>(2))
                jsonPath("$.items[0].testId", equalTo("BATCH-RUN-1"))
                jsonPath("$.items[0].priority", equalTo("High"))
                jsonPath("$.items[0].runStatus", equalTo("PASSED"))
                jsonPath("$.items[1].runStatus", equalTo("SKIPPED"))
            }
        mockMvc.get("/api/regressions/current")
            .andExpect {
                jsonPath("$.status", equalTo("RUNNING"))
                jsonPath("$.releaseName", equalTo("batch-release"))
                jsonPath("$.results.BATCH-RUN-1", equalTo("PASSED"))
                jsonPath("$.results.BATCH-RUN-2", equalTo("SKIPPED"))
            }
    }

    @Test
    fun `running batch requires run status for every item`() {
        startRegression("missing-status-release")

        mockMvc.post("/api/tests/batch?isRegressRunning=true") {
            contentType = MediaType.APPLICATION_JSON
            content = batchBody("BATCH-MISSING-STATUS", null)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("Required field runStatus is missing"))
            jsonPath("$.missingField", equalTo("runStatus"))
        }
    }

    @Test
    fun `batch update requires its own required fields instead of single upsert fallback`() {
        createTestCase("BATCH-STRICT")

        mockMvc.post("/api/tests/batch") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"items":[{"testId":"BATCH-STRICT"}]}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message", equalTo("Required field category is missing"))
            jsonPath("$.missingField", equalTo("category"))
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

    private fun batchBody(testId: String, runStatus: String?): String {
        val statusField = runStatus?.let { ",\"runStatus\":\"$it\"" }.orEmpty()
        return """
            {
              "items": [{
                "testId":"$testId",
                "category":"API",
                "shortTitle":"Batch contract",
                "scenario":{"steps":[{"number":1,"text":"step","attachments":[]}]}
                $statusField
              }]
            }
        """.trimIndent()
    }
}
