package org.golenev.restapi.config

import io.restassured.response.Response

class ResponseValidator(private val expectedStatus: Int) {
    fun validate(response: Response) {
        response.then()
            .log().all()
            .statusCode(expectedStatus)
    }
}
