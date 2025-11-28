package com.example.e2e.ui.config

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.WebDriverRunner
import com.codeborne.selenide.logevents.SelenideLogger
import io.qameta.allure.selenide.AllureSelenide
import org.openqa.selenium.MutableCapabilities
import org.openqa.selenium.chrome.ChromeOptions

object DriverConfig {

    fun setup() {
        Configuration.browserSize = "1920x1080"
        Configuration.timeout = 17_000
        Configuration.fastSetValue = true
        Configuration.pageLoadStrategy = "normal"
        Configuration.headless = false
        Configuration.screenshots = true
        Configuration.baseUrl = System.getProperty("baseUrl", "http://localhost:18080")

        SelenideLogger.addListener("AllureSelenide", AllureSelenide())

        val capabilities: MutableCapabilities =
            if (WebDriverRunner.isChrome()) getChromeOptions() else MutableCapabilities()

        Configuration.browserCapabilities = capabilities
    }

    private fun getChromeOptions(): ChromeOptions {
        val options = ChromeOptions()
        return options.addArguments("--window-size=1920,1080", "--disable-notifications")
    }
}
