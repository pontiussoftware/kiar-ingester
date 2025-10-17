package ch.pontius.kiar.database.institutions

import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.masterdata.Canton
import ch.pontius.kiar.database.collections.Collections
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.*

/**
 * A [Table] that holds information about [Institutions] that publish their objects.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Institutions : IntIdTable("institutions") {
    /** The name of the [Institutions] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** The unique identifier of a [Collections] entry. */
    val uuid = uuid("uuid").uniqueIndex().clientDefault { UUID.randomUUID() }

    /** The display name of the [Institutions] entry. */
    val displayName = varchar("display_name", 255)

    /** Flag indicating, that this entry should be published. */
    val publish = bool("publish").default(false)

    /** Reference to the [Participants] this [Institutions] belongs */
    val participantId = reference("participant_id", Participants)

    /** The optional ISIL of the [Institutions] entry. */
    val isil = varchar("isil", 255).nullable()

    /** The optional ISIL of the [Institutions] entry. */
    val description = text("description").nullable()

    /** The optional street of the [Institutions] entry. */
    val street = varchar("street", 255).nullable()

    /** The city of the [Institutions] entry. */
    val city = varchar("city", 255)

    /** The ZIP of the [Institutions] entry. */
    val zip = integer("zip")

    /** The canton of the [Institutions] entry. */
    val canton = enumerationByName<Canton>("canton", 2)

    /** The name held by this [Institutions] entry.*/
    val email = varchar("email", 255)

    /** The homepage of this [Institutions] entry.*/
    val homepage = varchar("homepage", 255).nullable()

    /** The longitude of this [Institutions] entry.*/
    val longitude = float("longitude").nullable()

    /** The latitude of this [Institutions] entry.*/
    val latitude = float("latitude").nullable()

    /** The optional street of the [Institutions] entry. */
    val imageName = varchar("image_path", 255).nullable()

    /** The optional default value for rights statement entries for this [Institutions] entry. */
    val defaultRightsStatement = varchar("default_rights_statement", 255).nullable()

    /** The optional default value for copyright entries for this [Institutions] entry. */
    val defaultCopyright = varchar("default_copyright", 255).nullable()

    /** The optional default object URL for this [Institutions] entry. */
    val defaultObjectUrl = varchar("default_object_url", 255).nullable()

    /** Timestamp of creation of the [Institutions] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of change of the [Institutions] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /**
     * Obtains a [Institutions] [id] by its [name],
     *
     * @param name The name to lookup
     * @return [Institutions] [id] or null, if no entry exists.
     */
    fun idByName(name: String) = Institutions.select(id).where { Institutions.name eq name}.map { it[id] }.firstOrNull()

    /**
     * Returns all [Institution]s stored in the database.
     *
     * @return [List] of [Institution] entries.
     */
    fun getAll(): List<Institution> = (Institutions innerJoin Participants).selectAll().map {
        it.toInstitution()
    }

    /**
     * Converts a [ResultRow] to a [Institution].
     *
     * @return [Institution]
     */
    fun ResultRow.toInstitution() = Institution(
        id = this[id].value,
        name = this[name],
        participantName = this[Participants.name],
        displayName = this[displayName],
        description = this[description],
        street = this[street],
        city = this[city],
        zip = this[zip],
        canton = this[canton],
        longitude = this[longitude],
        latitude = this[latitude],
        email = this[email],
        homepage = this[homepage],
        publish = this[publish],
        defaultRightStatement = this[defaultRightsStatement],
        defaultCopyright = this[defaultCopyright],
        defaultObjectUrl = this[defaultObjectUrl],
        imageName =  this[imageName],
        createdAt = this[created].toEpochMilli(),
        changedAt = this[modified].toEpochMilli()
    )
}