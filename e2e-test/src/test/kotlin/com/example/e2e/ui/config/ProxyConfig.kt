package com.example.e2e.ui.config

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverRunner
import com.codeborne.selenide.WebDriverRunner.getSelenideProxy
import io.qameta.allure.Step
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Predicate

object ProxyConfig {

    @Step("Подготовка прокси сервера")
    fun setUpProxy() {
        Configuration.proxyEnabled = true
    }

    @Step("Перехватываем запрос {0} и асинхронно возвращаем из него тело")
     fun interceptRequestBody(
        endpoint: String,
        run: () -> Unit,
    ): String = runBlocking {
        if (!WebDriverRunner.hasWebDriverStarted()) {
            open()
        }
        val deferredBody = CompletableDeferred<String>()
        getSelenideProxy().addRequestFilter(UUID.randomUUID().toString()) { _, httpMessageContents, httpMessageInfo ->
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

class ProxyInitializer() : Runnable {
    override fun run() {
        ProxyConfig.setUpProxy()
    }
}
