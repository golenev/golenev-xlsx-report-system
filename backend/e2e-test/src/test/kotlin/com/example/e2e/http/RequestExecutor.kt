package com.example.e2e.http

import io.qameta.allure.Step
import io.restassured.RestAssured
import io.restassured.http.Method
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

open class RequestExecutor<T : Any>(val path: String) : BaseSpecification() {

    protected fun getRequest(url: String, requestSpecification: RequestSpecification, expectedStatus: Int = 200): Response {
        val response: Response = prepareForRequest(requestSpecification)
            .request(Method.GET, baseUri + url)
        prepareForResponse(expectedStatus)
            .validate(response)
        return response
    }

    @Step("POST запрос к {url}")
    protected fun postRequest(url: String, requestSpecification: RequestSpecification, expectedStatus: Int = 200): Response {
        val response: Response = prepareForRequest(requestSpecification)
            .request(Method.POST, baseUri + url)
        prepareForResponse(expectedStatus)
            .validate(response)
        return response
    }

    protected fun baseRequest(): RequestSpecification = RestAssured.given()
}
