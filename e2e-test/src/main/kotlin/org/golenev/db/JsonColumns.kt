package org.golenev.db

import org.golenev.utils.JsonUtils
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb

inline fun <reified T : Any> Table.jsonbColumn(name: String): Column<T> =
    jsonb(name, { JsonUtils.objectMapper.writeValueAsString(it) }, { JsonUtils.objectMapper.readValue(it, T::class.java) })
