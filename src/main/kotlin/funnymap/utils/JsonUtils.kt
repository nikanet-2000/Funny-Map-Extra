package funnymap.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

fun JsonElement.toJsonArray(): JsonArray? = this as? JsonArray

fun JsonElement.toJsonObject(): JsonObject? = this as? JsonObject

fun JsonElement.toJsonPrimitive(): JsonPrimitive? = this as? JsonPrimitive

fun JsonObject.getJsonArray(member: String): JsonArray? = this.get(member)?.toJsonArray()

fun JsonObject.getJsonObject(member: String): JsonObject? = this.get(member)?.toJsonObject()

fun JsonObject.getJsonPrimitive(member: String): JsonPrimitive? = this.get(member)?.toJsonPrimitive()
