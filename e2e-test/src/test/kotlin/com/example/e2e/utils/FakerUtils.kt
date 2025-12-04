package com.example.e2e.utils

import net.datafaker.Faker
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs
import kotlin.random.Random


   private val faker: Faker = Faker(Locale("ru"))

    fun getRandomLong(): Long {
        return faker.number().numberBetween(1000000000L, 1999999999L)
    }

fun getRandomTestId(): Int {
    return faker.number().numberBetween(500, 19999)
}

    fun getRandomRuName(): String {
        return faker.name().name()
    }

    fun getRandomStreet(): String {
        return faker.address().streetAddress()
    }

    fun getRandomLongitude(): Double {
        return 30.0 + Random.nextDouble() * 30.0
    }

    fun getRandomLatitude(): Double {
        return 40.0 + Random.nextDouble() * 20.0
    }

    fun getRandomHouseNumber(): String {
        return faker.address().buildingNumber()
    }

    fun getRandomFullAddress() = "${getRandomStreet()}, ${getRandomHouseNumber()}"



