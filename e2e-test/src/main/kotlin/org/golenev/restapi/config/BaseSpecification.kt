package org.golenev.restapi.config

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.qameta.allure.restassured.AllureRestAssured
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.golenev.config.Environment

open class BaseSpecification(
    protected val baseUri: String = Environment.BASE_URI,
) {

    init {
        RestAssured.config = RestAssured.config().objectMapperConfig(
            ObjectMapperConfig().jackson2ObjectMapperFactory { _, _ ->
                jacksonObjectMapper()
                    .registerModule(JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        )
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    protected fun prepareForRequest(requestSpecification: RequestSpecification): RequestSpecification {
        return requestSpecification
            .baseUri(baseUri)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .log().all()
            .filter(AllureRestAssured())
    }

    protected fun prepareForResponse(expectedStatus: Int): ResponseValidator =
       ResponseValidator(expectedStatus)
}
