package ch.pontius.kiar.database.config

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.api.model.config.mappings.ValueParser
import ch.pontius.kiar.database.config.EntityMappings.created
import ch.pontius.kiar.database.config.EntityMappings.description
import ch.pontius.kiar.database.config.EntityMappings.modified
import ch.pontius.kiar.database.config.EntityMappings.name
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.json.json

/**
 * A [IntIdTable] that holds information about [AttributeMappings].
 *
 * [AttributeMappings] define how attributes from a given source should be mapped to the destination data model.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object AttributeMappings: Table("attribute_mappings") {
    /** Reference to the [EntityMappings] entry and [AttributeMappings] entry belongs to. */
    val entityMappingId = reference("entity_mapping_id", EntityMappings, ReferenceOption.CASCADE)

    /** The source of the [AttributeMappings] entry. Can be a XPath, JSONPath or column name. */
    val src = varchar("source", 255)

    /** The destination of the [AttributeMappings] entry. Corresponds to an Apache Solr Document field. */
    val destination = varchar("destination", 255)

    /** Flag indicating, that the field populated by an [AttributeMappings] entry is required. */
    val required = bool("required").default(false)

    /** Flag indicating, that the field populated by an [AttributeMappings] entry is multi-valued. */
    val multiValued = bool("multivalued").default(false)

    /** Enumeration of the [ValueParser] used by an [AttributeMappings] entry. */
    val parser = enumerationByName("parser", 32, ValueParser::class)

    /** Optional configuration parameters for an [AttributeMappings] entry. */
    val parameters = json<Map<String,String>>("parameters", Json).nullable()

    /**
     * Converts a [ResultRow] to a [AttributeMappings].
     *
     * @return [AttributeMappings]
     */
    fun ResultRow.toAttributeMapping() = AttributeMapping(
        source = this[src],
        destination = this[destination],
        required = this[required],
        multiValued = this[multiValued],
        parser = this[parser],
        parameters = this[parameters] ?: emptyMap()
    )
}