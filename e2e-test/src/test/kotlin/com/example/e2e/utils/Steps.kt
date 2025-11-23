package com.example.e2e.utils

import io.qameta.allure.Allure

fun <T> step(description: String, block: () -> T): T = Allure.step(description, block)
