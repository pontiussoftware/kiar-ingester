package ch.pontius.kiar.database.collections

import ch.pontius.kiar.api.model.collection.CollectionId
import ch.pontius.kiar.api.model.collection.ObjectCollection
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.api.model.user.UserId
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Institutions.toInstitution
import ch.pontius.kiar.database.institutions.Users
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.json.json
import java.util.UUID

/**
 * A [IntIdTable] that holds information about [Collections].
 *
 * [Collections] describe collections - as opposed to individual object - provided by [Institutions].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Collections: IntIdTable("collections") {
    /** Reference to the [Institutions] entry a [Collections] entry belongs to.*/
    val institutionId = reference("institution_id", Institutions,  onDelete = ReferenceOption.CASCADE)

    /** The unique identifier of a [Collections] entry. */
    val uuid = uuid("uuid").uniqueIndex().clientDefault { UUID.randomUUID() }

    /** The name of an [Collections] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** The display name of an [Collections] entry. */
    val displayName = varchar("display_name", 255)

    /** The description name of an [Collections] entry. */
    val description = text("description")

    /** The display name of an [Collections] entry. */
    val publish = bool("publish").default(false)

    /** Filter statements used by a [Collections] entry. */
    val filters = json<Array<String>>("filters", Json)

    /** Images associated with a [Collections] entry. */
    val images = json<Array<String>>("images", Json)

    /** Timestamp of creation of the [Institutions] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of change of the [Institutions] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /**
     * Returns an active [ObjectCollection] by its ID.
     *
     * @param id The [ObjectCollection]'s [CollectionId].
     */
    fun getById(collectionId: CollectionId) = (Collections innerJoin Institutions)
        .selectAll()
        .where { id eq collectionId }
        .map { it.toObjectCollection() }
        .firstOrNull()

    /**
     * Converts a [ResultRow] to a [ObjectCollection].
     *
     * @return [ObjectCollection]
     */
    fun ResultRow.toObjectCollection() = ObjectCollection(
        id = this[id].value,
        uuid = this[uuid].toString(),
        name = this[name],
        displayName = this[displayName],
        institution = this.getOrNull(Institutions.id)?.let { this.toInstitution() },
        description = this[description],
        publish = this[publish],
        filters = this[filters].toList(),
        images = this[images].toList(),
        createdAt = this[created].toEpochMilli(),
        changedAt = this[modified].toEpochMilli()
    )
}