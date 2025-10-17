package ch.pontius.kiar.migration.database.institution

import ch.pontius.kiar.api.model.masterdata.Canton
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.migration.database.config.solr.DbCollection
import ch.pontius.kiar.migration.database.job.DbJob
import ch.pontius.kiar.migration.database.masterdata.DbRightStatement
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.Instant

/**
 * A [DbInstitution] as managed by the KIAR Uploader Tool.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbInstitution(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbInstitution>() {
        fun migrate() {
            all().asSequence().forEach { dbInstitution ->
                Institutions.insert {
                    it[name] = dbInstitution.name
                    it[participantId] = Participants.idByName(dbInstitution.participant.name) ?: throw IllegalStateException("Unknown participant '${dbInstitution.participant.name}.")
                    it[displayName] = dbInstitution.displayName
                    it[isil] = dbInstitution.isil
                    it[description] = dbInstitution.description
                    it[street] = dbInstitution.street
                    it[city] = dbInstitution.city
                    it[zip] = dbInstitution.zip
                    it[canton] = Canton.entries.first { it.longName == dbInstitution.canton }
                    it[email] = dbInstitution.email
                    it[homepage] = dbInstitution.homepage
                    it[longitude] = dbInstitution.longitude
                    it[latitude] = dbInstitution.latitude
                    it[publish] = dbInstitution.publish
                    it[imageName] = dbInstitution.imageName
                    it[defaultRightsStatement] = dbInstitution.defaultRightStatement?.short
                    it[defaultCopyright] = dbInstitution.defaultCopyright
                    it[defaultObjectUrl] = dbInstitution.defaultObjectUrl
                    it[created] = dbInstitution.createdAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                    it[modified] = dbInstitution.changedAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                }
            }
        }
    }

    /** The name held by this [DbInstitution]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The [DbParticipant] this [DbInstitution] belongs to. */
    var participant by xdLink1(DbParticipant)

    /** The display name held by this [DbInstitution]. */
    var displayName by xdRequiredStringProp(trimmed = true)

    /** The ISIL number held by this [DbInstitution]. */
    var isil by xdStringProp(trimmed = true)

    /** A brief description for this [DbInstitution]. */
    var description by xdStringProp(trimmed = true)

    /** The name held by this [DbInstitution].*/
    var street by xdStringProp(trimmed = true)

    /** The name held by this [DbInstitution].*/
    var city by xdRequiredStringProp(trimmed = true)

    /** The ZIP code of this [DbInstitution].*/
    var zip by xdRequiredIntProp()

    /** The canton this [DbInstitution] belongs to. */
    var canton by xdRequiredStringProp(trimmed = true)

    /** The name held by this [DbInstitution].!*/
    var email by xdRequiredStringProp(trimmed = true)

    /** The name held by this [DbInstitution].!*/
    var homepage by xdStringProp(trimmed = true)

    /** The WGS84 longitude of the museum's location. */
    var longitude by xdNullableFloatProp()

    /** The WGS84 latitude of the museum's location. */
    var latitude by xdNullableFloatProp()

    /** Flag indicating whether this [DbInstitution]'s metadata should be published. */
    var publish by xdBooleanProp()

    /** Name of an image file. */
    var imageName by xdStringProp()

    /** The default value to use in the [Field.RIGHTS_STATEMENT] in case nothing was entered. */
    var defaultRightStatement by xdLink0_1(DbRightStatement, onTargetDelete = OnDeletePolicy.CLEAR)

    /** The default value to use in the [Field.COPYRIGHT] in case nothing was entered. */
    var defaultCopyright by xdStringProp(trimmed = true)

    /** The default value used to construct a direct URL to the object. */
    var defaultObjectUrl by xdStringProp(trimmed = true)

    /** The date and time this [DbInstitution] was created. */
    var createdAt by xdDateTimeProp()

    /** The date and time this [DbJob] was last changed. */
    var changedAt by xdDateTimeProp()

    /** List of [DbUser]s that belong to this [DbInstitution]. */
    val users by xdLink0_N(DbUser::institution, onDelete = OnDeletePolicy.CASCADE, onTargetDelete = OnDeletePolicy.CLEAR)

    /** List of [DbCollection]s that are available to this [DbInstitution] for publishing. */
    val availableCollections by xdLink0_N(DbCollection, onTargetDelete = OnDeletePolicy.CLEAR)

    /** List of [DbCollection]s that are have been selected by this [DbInstitution] for publishing. */
    val selectedCollections by xdLink0_N(DbCollection, onTargetDelete = OnDeletePolicy.CLEAR)
}