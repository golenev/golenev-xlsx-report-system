plugins {
    kotlin("jvm") version "1.9.23"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("io.rest-assured:rest-assured:5.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
