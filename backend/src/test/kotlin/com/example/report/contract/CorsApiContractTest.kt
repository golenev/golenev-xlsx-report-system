package com.example.report.contract

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.options

class CorsApiContractTest : ContractTestSupport() {

    @Test
    fun `api preflight exposes configured origin and methods`() {
        mockMvc.options("/api/tests") {
            header(HttpHeaders.ORIGIN, "https://client.example")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, equalTo("*")) }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")) }
        }
    }

    @Test
    fun `upload preflight exposes post method`() {
        mockMvc.options("/uploadReport") {
            header(HttpHeaders.ORIGIN, "https://client.example")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, equalTo("*")) }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")) }
        }
    }
}
