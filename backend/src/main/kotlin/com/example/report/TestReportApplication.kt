package com.example.report

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TestReportApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<TestReportApplication>(*args)
        }
    }
}
