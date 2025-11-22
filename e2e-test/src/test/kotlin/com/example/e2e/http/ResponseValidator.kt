package com.example.e2e.http

import io.restassured.response.Response

class ResponseValidator(private val expectedStatus: Int) {
    fun validate(response: Response) {
        response.then()
            .statusCode(expectedStatus)
    }
}
