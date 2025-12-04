package com.example.e2e.ui.config

import com.codeborne.selenide.WebDriverRunner.getSelenideProxy
import io.qameta.allure.Step
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.DisplayName
import java.util.*


@Step("Перехватываем запрос {0} и асинхронно возвращаем из него тело")
fun interceptRequestBody(
    endpoint: String,
    run: () -> Unit,
): String = runBlocking {
    withTimeout(15_000) {
        val deferredBody = CompletableDeferred<String>()
        getSelenideProxy().addRequestFilter(
            UUID.randomUUID().toString()
        ) { _, httpMessageContents, httpMessageInfo ->
            val isEndpointMatch = httpMessageInfo.url.contains(endpoint)
            val isPostMethod = httpMessageInfo.originalRequest.method().name().equals("post", ignoreCase = true)
            if (isEndpointMatch && isPostMethod && deferredBody.isActive) {
                deferredBody.complete(httpMessageContents.textContents)
            }
            null // не модифицируем запрос
        }
        run()
        deferredBody.await()
    }
}

@Step("Перехватываем ответ {0} и асинхронно возвращаем его тело")
fun interceptResponseBody(
    endpoint: String,
    run: () -> Unit,
): String = runBlocking {
    withTimeout(15_000) {
        val deferredBody = CompletableDeferred<String>()
        getSelenideProxy().addResponseFilter(
            UUID.randomUUID().toString()
        ) { _, httpMessageContents, httpMessageInfo ->
            val isEndpointMatch = httpMessageInfo.url.contains(endpoint)
            if (isEndpointMatch && deferredBody.isActive) {
                deferredBody.complete(httpMessageContents.textContents)
            }
        }
        run()
        deferredBody.await()
    }
}

@Step("Перехватываем ответ {0} и подставляем переданное тело")
fun replaceResponseBody(
    endpoint: String,
    responseBody: String,
    run: () -> Unit,
) {
    getSelenideProxy().addResponseFilter(UUID.randomUUID().toString()) { _, httpMessageContents, httpMessageInfo ->
        val isEndpointMatch = httpMessageInfo.url.contains(endpoint)
        if (isEndpointMatch) {
            httpMessageContents.textContents = responseBody
        }
    }
    run()
}



