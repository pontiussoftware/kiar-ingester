package ch.pontius.kiar.api.openapi

import io.ktor.openapi.OpenApiDoc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** The [Json] instance used to serialize the OpenAPI document. */
private val PRETTY = Json { prettyPrint = true }

/**
 * Serializes an [OpenApiDoc] to a JSON [String].
 *
 * Kotlinx serialization marks a property as required only when it has no default value. The previous
 * generator marked every non-nullable property as required, which is also what the API guarantees for
 * responses (defaults are always encoded). To keep the generated clients stable, the `required` list of
 * every component schema is rewritten to contain all non-nullable properties.
 *
 * @param doc The [OpenApiDoc] to serialize.
 * @return JSON representation.
 */
fun serializeOpenApiDoc(doc: OpenApiDoc): String {
    val root = Json.encodeToJsonElement(OpenApiDoc.serializer(), doc).jsonObject
    val components = root["components"]?.jsonObject ?: return PRETTY.encodeToString(JsonElement.serializer(), root)
    val schemas = components["schemas"]?.jsonObject ?: return PRETTY.encodeToString(JsonElement.serializer(), root)
    val rewritten = buildJsonObject {
        root.forEach { (key, value) ->
            if (key == "components") {
                put(key, buildJsonObject {
                    components.forEach { (ckey, cvalue) ->
                        if (ckey == "schemas") {
                            put(ckey, JsonObject(schemas.mapValues { (_, schema) -> schema.jsonObject.withRequiredNonNullable() }))
                        } else {
                            put(ckey, cvalue)
                        }
                    }
                })
            } else {
                put(key, value)
            }
        }
    }
    return PRETTY.encodeToString(JsonElement.serializer(), rewritten)
}

/**
 * Rewrites the `required` list of an object schema to contain all non-nullable properties.
 */
private fun JsonObject.withRequiredNonNullable(): JsonObject {
    val properties = this["properties"]?.jsonObject ?: return this
    val required = properties.filterValues { !it.isNullable() }.keys
    return buildJsonObject {
        this@withRequiredNonNullable.forEach { (key, value) -> if (key != "required") put(key, value) }
        if (required.isNotEmpty()) {
            put("required", JsonArray(required.map { JsonPrimitive(it) }))
        }
    }
}

/**
 * Checks whether a property schema permits null (either via a type union or a oneOf/anyOf with a null branch).
 */
private fun JsonElement.isNullable(): Boolean {
    val obj = this as? JsonObject ?: return false
    val type = obj["type"]
    if (type is JsonArray && type.any { (it as? JsonPrimitive)?.contentOrNull == "null" }) return true
    if (type is JsonPrimitive && type.contentOrNull == "null") return true
    for (key in listOf("oneOf", "anyOf")) {
        val branches = obj[key]?.jsonArray ?: continue
        if (branches.any { it.isNullable() }) return true
    }
    return false
}
