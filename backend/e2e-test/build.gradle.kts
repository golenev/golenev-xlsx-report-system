import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val exposedVersion = "0.49.0"
    val allureVersion = "2.25.0"

    testImplementation(kotlin("stdlib"))
    testImplementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    testImplementation("org.postgresql:postgresql:42.7.3")
    testImplementation("com.zaxxer:HikariCP:5.1.0")

    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.rest-assured:kotlin-extensions:5.4.0")
    testImplementation("io.qameta.allure:allure-junit5:$allureVersion")
    testImplementation("io.qameta.allure:allure-rest-assured:$allureVersion")
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

