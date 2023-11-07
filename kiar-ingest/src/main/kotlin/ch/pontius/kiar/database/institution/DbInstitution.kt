package ch.pontius.kiar.database.institution

import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.masterdata.DbRightStatement
import ch.pontius.kiar.ingester.solrj.Field
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence

/**
 * A [DbInstitution] as managed by the KIAR Uploader Tool.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbInstitution(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbInstitution>()

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
    var longitude by xdFloatProp()

    /** The WGS84 latitude of the museum's location. */
    var latitude by xdFloatProp()

    /** Flag indicating whether this [DbInstitution]'s metadata should be published. */
    var publish by xdBooleanProp()

    /** Name of an image file. */
    var imageName by xdStringProp()

    /** The default value to use in the [Field.RIGHTS_STATEMENT] in case nothing was entered. */
    var defaultRightStatement by xdLink0_1(DbRightStatement, onTargetDelete = OnDeletePolicy.CLEAR)

    /** The default value to use in the [Field.COPYRIGHT] in case nothing was entered. */
    var defaultCopyright by xdStringProp(trimmed = true)

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

    /**
     * Convenience method to convert this [DbInstitution] to a [Institution].
     *
     * Requires an ongoing transaction.
     *
     * @return [Institution]
     */
    fun toApi(): Institution = Institution(
        id = this.xdId,
        name = this.name,
        displayName = this.displayName,
        participantName = this.participant.name,
        description = this.description,
        isil = this.isil,
        street = this.street,
        city = this.city,
        zip = this.zip,
        canton = this.canton,
        longitude = this.longitude,
        latitude = this.latitude,
        email = this.email,
        homepage = this.homepage,
        publish = this.publish,
        availableCollections = this.availableCollections.asSequence().map { it.name }.toList(),
        selectedCollections = this.selectedCollections.asSequence().map { it.name  }.toList(),
        defaultRightStatement = this.defaultRightStatement?.short,
        defaultCopyright = this.defaultCopyright,
        imageName = this.imageName,
        createdAt = this.createdAt?.millis,
        changedAt = this.createdAt?.millis
    )
}