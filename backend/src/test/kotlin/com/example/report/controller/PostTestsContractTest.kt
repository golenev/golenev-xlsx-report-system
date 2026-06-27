package com.example.report.controller

import com.example.report.repository.TestReportRepository
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostTestsContractTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testReportRepository: TestReportRepository

    @BeforeEach
    fun cleanDb() {
        testReportRepository.deleteAll()
    }

    /**
     * Контрактный happy path проверяет эквивалентное классу валидных POST /api/tests тело: structured scenario
     * должен приниматься как вложенный JSON-объект, а не как строка, и возвращаться из GET без двойной сериализации.
     */
    @Test
    fun `post tests accepts structured scenario object and returns object contract`() {
        mockMvc.post("/api/tests?forceUpdate=true") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "testId": "CONTRACT-1",
                  "category": "E2E_FOR_AUTOTEST",
                  "shortTitle": "Batch creates 10 items and report shows them",
                  "issueLink": "https://youtrack.test/issue/CONTRACT-1",
                  "readyDate": "2026-06-25",
                  "generalStatus": "Готово",
                  "priority": "Medium",
                  "scenario": {
                    "steps": [
                      {
                        "number": 1,
                        "text": "Формируем batch из десяти тест-кейсов",
                        "attachments": [
                          {"type": "text", "content": "Batch содержит 10 элементов.\nКаждый testId уникален."}
                        ]
                      },
                      {"number": 2, "text": "Проверяем отчёт", "attachments": []}
                    ]
                  },
                  "notes": ""
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tests"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].scenario.steps[0].number", equalTo(1)))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].scenario.steps[0].attachments[0].content", containsString("Каждый testId уникален")))
    }

    /**
     * Негативный контракт использует анализ типов: scenario-строка внешне содержит JSON, но нарушает схему API,
     * поэтому POST /api/tests обязан вернуть 400 и не сохранять несовместимый формат.
     */
    @Test
    fun `post tests rejects scenario sent as json string`() {
        mockMvc.post("/api/tests") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "testId": "CONTRACT-STRING",
                  "category": "API",
                  "shortTitle": "String scenario must fail",
                  "scenario": "{\"steps\":[{\"number\":1,\"text\":\"bad\",\"attachments\":[]}]}"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }

    /**
     * Граничная проверка обязательных полей внутри массива steps: минимальный невалидный шаг без attachments
     * должен отклоняться, чтобы клиент и БД не получили частично нормализованный scenario.
     */
    @Test
    fun `post tests rejects step without attachments array`() {
        mockMvc.post("/api/tests") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "testId": "CONTRACT-NO-ATTACHMENTS",
                  "category": "API",
                  "shortTitle": "Attachments required",
                  "scenario": {"steps": [{"number": 1, "text": "Шаг без массива attachments"}]}
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
