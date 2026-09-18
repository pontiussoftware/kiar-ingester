package ch.pontius.kiar.api.openapi

import ch.pontius.kiar.api.model.status.ErrorStatus
import io.ktor.http.ContentType
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonType
import io.ktor.openapi.Operation
import io.ktor.openapi.Parameters
import io.ktor.openapi.ReferenceOr
import io.ktor.openapi.Responses
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.openapi.RouteOperationFunction

/*
 * Small DSL helpers on top of Ktor's OpenAPI [Operation.Builder] so that each route description stays
 * about as compact as the annotations it replaces.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */

/** Alias for a function describing a route's OpenAPI operation. */
typealias RouteDoc = RouteOperationFunction

/** JSON schema for a 32-bit integer. */
val INT32 = JsonSchema(type = JsonType.INTEGER, format = "int32")

/** JSON schema for a string. */
val STRING = JsonSchema(type = JsonType.STRING)

/** JSON schema for a boolean. */
val BOOLEAN = JsonSchema(type = JsonType.BOOLEAN)

/** JSON schema for binary content. */
val BINARY = JsonSchema(type = JsonType.STRING, format = "binary")

/** JSON schema for an [ErrorStatus]. */
private val ERROR_STATUS by lazy { schemaOf<ErrorStatus>() }

/** Reason phrases as emitted by the previous OpenAPI generator. */
@PublishedApi
internal val REASONS = mapOf(
    200 to "OK",
    400 to "Bad Request",
    401 to "Unauthorized",
    403 to "Forbidden",
    404 to "Not Found",
    500 to "Server Error"
)

/**
 * Infers the [JsonSchema] for type [T] using kotlinx serialization descriptors.
 */
inline fun <reified T : Any> schemaOf(): JsonSchema = KiarSchemaInference.jsonSchema<T>()

/**
 * Adds multiple tags to the operation.
 */
fun Operation.Builder.tags(vararg tags: String) = tags.forEach { this.tag(it) }

/**
 * Adds a required path parameter.
 */
fun Parameters.Builder.pathParam(name: String, description: String, schema: JsonSchema = INT32) = this.path(name) {
    this.description = description
    this.required = true
    this.schema = schema
}

/**
 * Adds a query parameter.
 */
fun Parameters.Builder.queryParam(name: String, description: String, schema: JsonSchema = STRING, required: Boolean = false) = this.query(name) {
    this.description = description
    this.required = required
    this.schema = schema
}

/**
 * Declares a required JSON request body of type [T].
 */
inline fun <reified T : Any> Operation.Builder.jsonBody(description: String? = null) = this.requestBody {
    this.required = true
    this.description = description
    this.schema = schemaOf<T>()
}

/**
 * Declares a required multipart/form-data request body with a single binary file field.
 */
fun Operation.Builder.multipartFile(fieldName: String, description: String) = this.requestBody {
    this.required = true
    this.description = description
    ContentType.MultiPart.FormData {
        this.schema = JsonSchema(type = JsonType.OBJECT, properties = mapOf(fieldName to ReferenceOr.Value(BINARY)))
    }
}

/**
 * Declares a required application/x-www-form-urlencoded request body with the given string fields.
 */
fun Operation.Builder.formBody(description: String, vararg fields: String) = this.requestBody {
    this.required = true
    this.description = description
    ContentType.Application.FormUrlEncoded {
        this.schema = JsonSchema(type = JsonType.OBJECT, properties = fields.associateWith { ReferenceOr.Value(STRING) })
    }
}

/**
 * Declares a JSON response of type [T] for the given status code.
 */
inline fun <reified T : Any> Responses.Builder.json(code: Int = 200) = this.response(code) {
    this.description = REASONS[code] ?: ""
    this.schema = schemaOf<T>()
}

/**
 * Declares [ErrorStatus] JSON responses for the given status codes.
 */
fun Responses.Builder.errors(vararg codes: Int) = codes.forEach { code ->
    this.response(code) {
        this.description = REASONS[code] ?: ""
        this.schema = ERROR_STATUS
    }
}

/**
 * Declares a text/xml response for the given status code.
 */
fun Responses.Builder.xml(code: Int = 200) = this.response(code) {
    this.description = REASONS[code] ?: ""
    ContentType.Text.Xml { }
}

/**
 * Declares a binary image (JPEG or PNG) response for the given status code.
 */
fun Responses.Builder.image(code: Int = 200) = this.response(code) {
    this.description = REASONS[code] ?: ""
    ContentType.Image.JPEG { this.schema = BINARY }
    ContentType.Image.PNG { this.schema = BINARY }
}
