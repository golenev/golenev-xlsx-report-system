package com.example.e2e.ui.config

import com.codeborne.selenide.WebDriverRunner.getSelenideProxy
import com.codeborne.selenide.proxy.SelenideProxyServer
import io.qameta.allure.Step
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.DisplayName
import java.util.*


@Step("Перехватываем запрос {0} и асинхронно возвращаем из него тело")
fun interceptRequestBody(
    proxyServer: SelenideProxyServer,
    endpoint: String,
    run: () -> Unit,
): String = runBlocking {
    withTimeout(15_000) {
        val deferredBody = CompletableDeferred<String>()
        proxyServer.addRequestFilter(
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
    proxyServer: SelenideProxyServer,
    endpoint: String,
    run: () -> Unit,
): String = runBlocking {
    withTimeout(15_000) {
        val deferredBody = CompletableDeferred<String>()
        proxyServer.addResponseFilter(
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
    proxyServer: SelenideProxyServer,
    endpoint: String,
    responseBody: String,
    run: () -> Unit,
) {
    proxyServer.addResponseFilter(UUID.randomUUID().toString()) { _, httpMessageContents, httpMessageInfo ->
        val isEndpointMatch = httpMessageInfo.url.contains(endpoint)
        if (isEndpointMatch) {
            httpMessageContents.textContents = responseBody
        }
    }
    run()
}



