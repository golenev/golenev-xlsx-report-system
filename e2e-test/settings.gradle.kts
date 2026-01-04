rootProject.name = "e2e-test"

includeBuild("../allure-helpers") {
    dependencySubstitution {
        substitute(module("com.example:allure-helpers")).using(project(":"))
    }
}
