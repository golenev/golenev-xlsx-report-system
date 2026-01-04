plugins {
    kotlin("jvm") version "1.9.23"
    id("io.qameta.allure") version "2.12.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val exposedVersion = "0.49.0"
    val allureVersion = "2.25.0"
    val selenideVersion = "7.12.1"
    val fakerVersion = "2.2.2"

    testImplementation(kotlin("stdlib"))
    testImplementation("com.example:allure-helpers")
    testImplementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    testImplementation("org.postgresql:postgresql:42.7.3")
    testImplementation("com.zaxxer:HikariCP:5.1.0")

    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.rest-assured:kotlin-extensions:5.4.0")

    testImplementation("io.qameta.allure:allure-junit5:$allureVersion")
    testImplementation("io.qameta.allure:allure-rest-assured:$allureVersion")
    testImplementation("io.qameta.allure:allure-junit5:2.29.1")
    testImplementation("io.qameta.allure:allure-selenide:2.29.1")

    testImplementation("com.codeborne:selenide:$selenideVersion")
    testImplementation("com.codeborne:selenide-proxy:7.12.1")
    testImplementation("net.datafaker:datafaker:$fakerVersion")


    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

allure {
    report {
        version.set("2.29.0")
    }
    adapter {
        autoconfigure.set(true)
        frameworks {
            junit5 {
                adapterVersion.set("2.29.0")
            }
        }
    }
}

tasks.register("deleteAllureReport", Delete::class) {
    delete(rootProject.layout.buildDirectory.dir("reports/allure-report"))
}

tasks.named<Test>("test") {
    dependsOn(tasks.named("deleteAllureReport"))
    useJUnitPlatform()
    finalizedBy(tasks.named("allureReport"))
}

val allureTestCasesPath = layout.buildDirectory.dir("reports/allure-report/allureReport/data/test-cases")

tasks.register<JavaExec>("runMyKotlinFunction") {
    group = "custom"
    description = "Runs a Kotlin function from test sources"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("helpers.MyRunner")
    dependsOn("testClasses")
    systemProperty("allure.testCasesPath", allureTestCasesPath.get().asFile.absolutePath)
}