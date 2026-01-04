rootProject.name = "test-report-backend"

includeBuild("../allure-helpers") {
    dependencySubstitution {
        substitute(module("com.example:allure-helpers")).using(project(":"))
    }
}
