package com.example.report.tests

import com.example.report.service.allure.TestCaseModel
import com.example.report.service.allure.parseAllureReportsFromFolder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class AllureTestCasesIntegrationTest {

    private val mapper = jacksonObjectMapper()
    private val baseUrl: String = System.getenv("ALLURE_API_BASE_URL")
        ?: System.getProperty("allure.api.base-url")
        ?: "http://localhost:8080"

    @Test
    fun `should return parsed allure test cases`() {
        val folderPath = System.getenv("ALLURE_TEST_CASES_PATH")
            ?: System.getProperty("allure.test-cases.path")
            ?: Paths.get(
                "src",
                "test",
                "resources",
                "allure",
                "test-cases"
            ).toAbsolutePath().toString()

        val response = Given {
            baseUri(baseUrl)
            basePath("/api")
            accept(ContentType.JSON)
            queryParam("path", folderPath)
        } When {
            get("/allure/test-cases")
        } Then {
            statusCode(200)
        } Extract {
            asString()
        }

        val actual: List<TestCaseModel> = mapper.readValue(response)
        val expected = parseAllureReportsFromFolder(folderPath)

        assertThat(actual).containsExactlyElementsOf(expected)
    }

}
