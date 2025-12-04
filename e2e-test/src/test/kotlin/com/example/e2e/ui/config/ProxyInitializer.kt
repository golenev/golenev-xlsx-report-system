package com.example.e2e.ui.config

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.WebDriverRunner

class ProxyInitializer() : Runnable {
    override fun run() {
        Configuration.proxyEnabled = true
        if (!WebDriverRunner.hasWebDriverStarted()) {
            open("/")
        }
    }
}