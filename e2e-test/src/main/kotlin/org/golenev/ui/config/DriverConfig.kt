package org.golenev.ui.config

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.WebDriverRunner
import com.codeborne.selenide.logevents.LogEventListener
import com.codeborne.selenide.logevents.SelenideLogger
import org.openqa.selenium.MutableCapabilities
import org.golenev.ui.allure.ReadableAllureSelenideListener
import org.golenev.ui.allure.UiElementNameRegistry
import org.openqa.selenium.chrome.ChromeOptions

class DriverConfig {

    fun setup() {
        Configuration.browserSize = "1920x1080"
        Configuration.timeout = 6_000
        Configuration.fastSetValue = true
        Configuration.pageLoadStrategy = "normal"
        Configuration.headless = false
        Configuration.screenshots = true
        Configuration.baseUrl = System.getProperty("baseUrl", "http://localhost:18080")
        Configuration.proxyEnabled = true

        UiElementNameRegistry.clear()

        SelenideLogger.removeListener<LogEventListener>("AllureSelenide")
        SelenideLogger.removeListener<LogEventListener>("ReadableAllureSelenide")

        SelenideLogger.addListener(
            "ReadableAllureSelenide",
            ReadableAllureSelenideListener()
        )

        val capabilities: MutableCapabilities =
            if (WebDriverRunner.isChrome()) getChromeOptions() else MutableCapabilities()

        Configuration.browserCapabilities = capabilities
    }

    private fun getChromeOptions(): ChromeOptions {
        val options = ChromeOptions()
        return options.addArguments("--window-size=1920,1080", "--disable-notifications")
    }
}
