package com.example.e2e.ui.config

import com.codeborne.selenide.proxy.SelenideProxyServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.*


//Перехватываем запрос и асинхронно возвращаем из него тело
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

//Перехватываем ответ и асинхронно возвращаем его тело
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

//Перехватываем ответ  подставляем переданное тело
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



