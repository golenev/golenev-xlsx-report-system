package org.golenev.ui.config

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.WebDriverRunner
import com.codeborne.selenide.logevents.LogEventListener
import com.codeborne.selenide.logevents.SelenideLogger
import org.openqa.selenium.MutableCapabilities
import org.openqa.selenium.chrome.ChromeOptions

class DriverConfig {

    fun setup() {
        Configuration.browserSize = "1920x1080"
        Configuration.timeout = 6_000
        Configuration.fastSetValue = true
        Configuration.pageLoadStrategy = "normal"
        Configuration.headless = false
        Configuration.screenshots = true
        Configuration.baseUrl = System.getProperty(
            "baseUrl",
            "http://localhost:18080"
        )
        Configuration.proxyEnabled = true

        configureSelenideListener()

        Configuration.browserCapabilities =
            if (WebDriverRunner.isChrome()) {
                getChromeOptions()
            } else {
                MutableCapabilities()
            }
    }

    private fun configureSelenideListener() {
        UiElementMetadataRegistry.clear()

        SelenideLogger.removeListener<LogEventListener>(
            DEFAULT_ALLURE_LISTENER
        )

        SelenideLogger.removeListener<LogEventListener>(
            CUSTOM_ALLURE_LISTENER
        )

        SelenideLogger.addListener(
            CUSTOM_ALLURE_LISTENER,
            CustomAllureSelenideListener(),
        )
    }

    private fun getChromeOptions(): ChromeOptions {
        return ChromeOptions().addArguments(
            "--window-size=1920,1080",
            "--disable-notifications",
        )
    }

    private companion object {

        const val DEFAULT_ALLURE_LISTENER = "AllureSelenide"

        const val CUSTOM_ALLURE_LISTENER = "ReadableAllureSelenide"
    }
}
