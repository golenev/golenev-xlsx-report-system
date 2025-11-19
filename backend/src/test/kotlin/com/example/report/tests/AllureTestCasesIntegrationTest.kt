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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
    ]
)
class AllureTestCasesIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private val mapper = jacksonObjectMapper()

    @Test
    fun `should return parsed allure test cases`() {
        val folderPath = resolveAllureFolderPath()

        val response = Given {
            port(port)
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
    private fun resolveAllureFolderPath(): String {
        val fromSystemProperty = System.getProperty("allure.testcases.path")
        val fromEnv = System.getenv("ALLURE_TEST_CASES_PATH")

        return listOfNotNull(fromSystemProperty, fromEnv)
            .firstOrNull()
            ?: "C:/Users/inter/IdeaProjects/motivation-service-tests/build/reports/allure-report/allureReport/data/test-cases"
    }
}
