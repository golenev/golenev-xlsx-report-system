package helpers

import io.restassured.RestAssured
import io.restassured.http.ContentType
import kotlin.system.exitProcess
import java.io.File


object MyRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        println(">>> Start MyRunner")
        try {
            val testCasesPath = System.getProperty("allure.testCasesPath")
                ?: defaultAllurePathOrError()

            val items = parseAllureReportsFromFolder(testCasesPath)
            println(">>> Parsed test cases: ${items.size}")

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(
                    BatchRequest(
                        items = items.map { testCase ->
                            TestCaseItem(
                                testId = testCase.id,
                                category = testCase.category,
                                shortTitle = testCase.name,
                                issueLink = "",
                                readyDate = "",
                                generalStatus = "Готово",
                                scenario = testCase.scenario,
                                notes = "",
                                priority = "Medium"
                            )
                        }
                    )
                )
                .log().all()
                .post("http://localhost:18080/api/tests/batch")
                .then()
                .log().all()
                .statusCode(200)
            println(">>> Finished successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(12074)
        }
    }

    private fun defaultAllurePathOrError(): String {
        val defaultPath = File("build/reports/allure-report/allureReport/data/test-cases")
        if (defaultPath.exists() && defaultPath.isDirectory) {
            return defaultPath.absolutePath
        }
        error(
            "System property 'allure.testCasesPath' is not set. " +
                "Either run Gradle task 'runMyKotlinFunction' or provide the property manually."
        )
    }
}
