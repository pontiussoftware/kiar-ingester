package ch.pontius.kiar.api.openapi

import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonSchemaInference
import io.ktor.openapi.JsonType
import io.ktor.openapi.KotlinxSerializerJsonSchemaInference
import io.ktor.openapi.ReferenceOr
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KType

/**
 * A [JsonSchemaInference] that refines the schemas inferred by Ktor from kotlinx serialization descriptors so that
 * the resulting OpenAPI document matches what the previous generator emitted for this API:
 *
 * - Enums carry a title and thus become named component schemas (instead of being inlined).
 * - Integer and floating point numbers carry a `format` (`int32`, `int64`, `float`, `double`).
 * - Nullable references to component schemas are expressed as `oneOf [ref, null]` instead of marking the
 *   (shared) component schema itself as nullable.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@OptIn(ExperimentalSerializationApi::class)
object KiarSchemaInference : JsonSchemaInference {

    /** The delegate [JsonSchemaInference]. */
    private val delegate = KotlinxSerializerJsonSchemaInference(EmptySerializersModule())

    override fun buildSchema(type: KType): JsonSchema {
        val schema = this.delegate.buildSchema(type)
        val descriptor = serializer(type).descriptor
        return refine(schema, descriptor, mutableSetOf())
    }

    /**
     * Refines a [JsonSchema] by walking it alongside the [SerialDescriptor] it was generated from.
     */
    private fun refine(schema: JsonSchema, descriptor: SerialDescriptor, visiting: MutableSet<String>): JsonSchema = when (descriptor.kind) {
        StructureKind.CLASS, StructureKind.OBJECT -> {
            val name = descriptor.nonNullSerialName
            val properties = schema.properties
            if (properties == null || !visiting.add(name)) {
                schema.asNonNullObject()
            } else {
                try {
                    val refined = properties.toMutableMap()
                    for (i in 0 until descriptor.elementsCount) {
                        val propertyName = descriptor.getElementName(i)
                        val propertyDescriptor = descriptor.getElementDescriptor(i)
                        val property = refined[propertyName] ?: continue
                        refined[propertyName] = refineProperty(property, propertyDescriptor, visiting)
                    }
                    schema.copy(properties = refined).asNonNullObject()
                } finally {
                    visiting.remove(name)
                }
            }
        }
        StructureKind.LIST -> {
            val items = schema.items
            if (items != null) {
                schema.copy(items = refineProperty(items, descriptor.getElementDescriptor(0), visiting))
            } else {
                schema
            }
        }
        SerialKind.ENUM -> schema.copy(title = descriptor.nonNullSerialName, type = JsonType.STRING)
        PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT -> schema.withFormat("int32")
        PrimitiveKind.LONG -> schema.withFormat("int64")
        PrimitiveKind.FLOAT -> schema.withFormat("float")
        PrimitiveKind.DOUBLE -> schema.withFormat("double")
        else -> schema
    }

    /**
     * Refines the schema of a property (or list item), re-introducing nullability for component references.
     */
    private fun refineProperty(property: ReferenceOr<JsonSchema>, descriptor: SerialDescriptor, visiting: MutableSet<String>): ReferenceOr<JsonSchema> {
        val isComponent = descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT || descriptor.kind == SerialKind.ENUM
        val refined: ReferenceOr<JsonSchema> = when (property) {
            is ReferenceOr.Value -> {
                val value = property.value
                /* Unwrap an existing oneOf [X, null] produced by the delegate. */
                val branches = value.oneOf
                if (value.type == null && branches != null && branches.size == 2 && branches.any { it.isNullType() }) {
                    val inner = branches.first { !it.isNullType() }
                    return wrapNullable(refineProperty(inner, descriptor, visiting))
                }
                ReferenceOr.Value(refine(value, descriptor, visiting))
            }
            is ReferenceOr.Reference -> property
        }
        return if (descriptor.isNullable && isComponent) wrapNullable(refined) else refined
    }

    /**
     * Wraps a schema (or reference) into `oneOf [schema, null]`.
     */
    private fun wrapNullable(schema: ReferenceOr<JsonSchema>): ReferenceOr<JsonSchema> = ReferenceOr.Value(
        JsonSchema(oneOf = listOf(schema, ReferenceOr.Value(JsonSchema(type = JsonType.NULL))))
    )

    /** Checks whether this is a `{ "type": "null" }` schema. */
    private fun ReferenceOr<JsonSchema>.isNullType(): Boolean = this is ReferenceOr.Value && this.value.type == JsonType.NULL

    /** Forces an object schema's type to a plain (non-nullable) `object`. */
    private fun JsonSchema.asNonNullObject(): JsonSchema = if (this.type != null) this.copy(type = JsonType.OBJECT) else this

    /** Sets the format of a numeric schema, if not set already. */
    private fun JsonSchema.withFormat(format: String): JsonSchema = if (this.format == null) this.copy(format = format) else this

    /** The serial name of a descriptor without the nullability marker. */
    private val SerialDescriptor.nonNullSerialName: String get() = this.serialName.trimEnd('?')
}
