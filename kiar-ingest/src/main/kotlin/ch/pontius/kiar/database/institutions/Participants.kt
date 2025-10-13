package ch.pontius.kiar.database.institutions

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.select

/**
 * A [Table] that holds information about participants.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Participants: IntIdTable("participants") {
    /** The name of the [Institutions] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** Timestamp of creation of the [Participants] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of change of the [Participants] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /**
     * Obtains a [Participants] [id] by its [name]-
     *
     * @param name The name to lookup
     * @return [Participants] [id] or null, if no entry exists.
     */
    fun idByName(name: String) =  Participants.select(id).where { Participants.name eq name }.map { it[id] }.firstOrNull()
}