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
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("io.rest-assured:rest-assured:5.4.0")
    implementation("io.rest-assured:kotlin-extensions:5.4.0")

    implementation("io.qameta.allure:allure-junit5:$allureVersion")
    implementation("io.qameta.allure:allure-rest-assured:$allureVersion")
    implementation("io.qameta.allure:allure-selenide:2.29.1")

    implementation("com.codeborne:selenide:$selenideVersion")
    implementation("com.codeborne:selenide-proxy:7.12.1")
    implementation("net.datafaker:datafaker:$fakerVersion")


    implementation("io.kotest:kotest-assertions-core:5.9.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    implementation(platform("org.junit:junit-bom:5.10.2"))
    implementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("user.timezone", "Europe/Moscow")
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
