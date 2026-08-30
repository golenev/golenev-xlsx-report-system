package com.example.report.contract

import com.example.report.repository.RegressionRepository
import com.example.report.repository.TestReportRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ContractClockConfiguration::class)
abstract class ContractTestSupport {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var testReportRepository: TestReportRepository

    @Autowired
    protected lateinit var regressionRepository: RegressionRepository

    @BeforeEach
    fun cleanDatabase() {
        regressionRepository.deleteAll()
        testReportRepository.deleteAll()
    }

    protected fun createTestCase(
        testId: String,
        category: String = "API",
        shortTitle: String = "Contract test $testId",
        forceUpdate: Boolean = true,
        issueLink: String? = "https://issue/$testId",
        readyDate: String? = "2026-01-15",
        generalStatus: String? = "Готово",
        priority: String? = "Medium",
        notes: String? = "notes $testId",
    ) {
        val optionalFields = buildString {
            issueLink?.let { append("\"issueLink\":\"$it\",") }
            readyDate?.let { append("\"readyDate\":\"$it\",") }
            generalStatus?.let { append("\"generalStatus\":\"$it\",") }
            priority?.let { append("\"priority\":\"$it\",") }
            notes?.let { append("\"notes\":\"$it\",") }
        }
        mockMvc.post("/api/tests") {
            param("forceUpdate", forceUpdate.toString())
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "testId":"$testId",
                  "category":"$category",
                  "shortTitle":"$shortTitle",
                  $optionalFields
                  "scenario":{"steps":[{"number":1,"text":"step $testId","attachments":[]}]}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }
    }
}

@TestConfiguration
class ContractClockConfiguration {

    @Bean
    @Primary
    fun contractClock(): Clock = Clock.fixed(
        Instant.parse("2026-06-28T10:15:30Z"),
        ZoneId.of("Europe/Moscow"),
    )
}
