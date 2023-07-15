package ch.pontius.kiar.database.institution

import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.database.job.DbJob
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy

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

    /** Flag indicating whether this [DbInstitution]'s metadata should be published. */
    var publish by xdBooleanProp()

    /** The date and time this [DbInstitution] was created. */
    var createdAt by xdDateTimeProp()

    /** The date and time this [DbJob] was last changed. */
    var changedAt by xdDateTimeProp()

    /** List of [DbUser]s that belong to this [DbInstitution]. */
    val users by xdLink0_N(DbUser::institution, onDelete = OnDeletePolicy.CASCADE, onTargetDelete = OnDeletePolicy.CLEAR)

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
        email = this.email,
        homepage = this.homepage,
        publish = this.publish,
        createdAt = this.createdAt?.millis,
        changedAt = this.createdAt?.millis
    )
}