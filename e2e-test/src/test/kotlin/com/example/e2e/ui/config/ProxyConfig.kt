package com.example.e2e.ui.config

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide.getSelenideProxy
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverRunner
import io.qameta.allure.Step
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Predicate

object ProxyConfig {

    @Step("Подготовка прокси сервера")
    fun setUpProxy(uniqueTestName: String) {
        Configuration.proxyEnabled = true

        if (!WebDriverRunner.hasWebDriverStarted()) {
            open()
        }
    }

    @Step("Перехватываем запрос {0} и асинхронно возвращаем из него тело")
    fun interceptRequestBody(
        endpoint: String,
        additionalMatcher: Predicate<String> = Predicate { true },
        run: () -> Unit,
    ): String {
        val selenideProxyServer = getSelenideProxy()
        val future = CompletableFuture<String>()
        val filterName = "requestProxy.dataGetter-${UUID.randomUUID()}"

        selenideProxyServer.addRequestFilter(filterName) { _, httpMessageContents, httpMessageInfo ->
            val isEndpointMatch = httpMessageInfo.url.contains(endpoint)
            val isAdditionalMatch = additionalMatcher.test(httpMessageInfo.url)
            val isPostMethod = httpMessageInfo.originalRequest().method().name().equals("post", true)

            if (isEndpointMatch && isAdditionalMatch && isPostMethod) {
                future.complete(httpMessageContents.textContents)
            }
            null
        }

        run()

        return future.get(5, TimeUnit.SECONDS)
    }
}

class ProxyInitializer(private val uniqueTestName: String) : Runnable {
    override fun run() {
        ProxyConfig.setUpProxy(uniqueTestName)
    }
}
