package ch.pontius.kiar.database.config

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.api.model.config.mappings.MappingFormat
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.select

/**
 * A [IntIdTable] that holds information about [EntityMappings].
 *
 * [EntityMappings] group [AttributeMappings] into a named mapping logic.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object EntityMappings: IntIdTable("entity_mappings") {
    /** The name of the [EntityMappings] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** The optional description of the [EntityMappings] entry. */
    val description = text("description").nullable()

    /** The [MappingFormat] of a [EntityMappings] entry. */
    val format = enumerationByName("format", 8, MappingFormat::class)

    /** Timestamp of creation of the [EntityMappings] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of change of the [EntityMappings] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /**
     * Obtains a [EntityMappings] [id] by its [name].
     *
     * @param name The name to lookup
     * @return [EntityMappings] [id] or null, if no entry exists.
     */
    fun idByName(name: String) = EntityMappings.select(id).where { EntityMappings.name eq name }.map { it[id].value }.firstOrNull()

    /**
     * Converts a [ResultRow] to a [EntityMappings].
     *
     * @return [EntityMappings]
     */
    fun ResultRow.toEntityMapping() = EntityMapping(
        id = this[id].value,
        name = this[name],
        description = this[description],
        type = this[format],
        createdAt = this[created].toEpochMilli(),
        changedAt = this[modified].toEpochMilli(),
    )
}