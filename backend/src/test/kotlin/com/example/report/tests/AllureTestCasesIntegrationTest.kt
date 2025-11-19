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

class AllureTestCasesIntegrationTest {

    private val mapper = jacksonObjectMapper()
    private val baseUrl: String = "http://localhost:18080"

    @Test
    fun `should return parsed allure test cases`() {
        val folderPath = "C:/Users/inter/IdeaProjects/motivation-service-tests/build/reports/allure-report/allureReport/data/test-cases"

        val response = Given {
            baseUri(baseUrl)
            accept(ContentType.JSON)
            queryParam("path", folderPath)
        } When {
            get("/api/allure/test-cases")
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
