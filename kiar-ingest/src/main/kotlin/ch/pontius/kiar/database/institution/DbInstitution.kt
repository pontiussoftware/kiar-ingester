package ch.pontius.kiar.database.institution

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
    val participant by xdLink1(DbParticipant)

    /** The display name held by this [DbInstitution]. */
    var displayName by xdRequiredStringProp(trimmed = true)

    /** The ISIL number held by this [DbInstitution]. */
    var isil by xdStringProp(trimmed = true)

    /** A brief description for this [DbInstitution]. */
    var description by xdStringProp(trimmed = true)

    /** The name held by this [DbInstitution].*/
    var street by xdStringProp(trimmed = true)

    /** The name held by this [DbInstitution].*/
    var city by xdStringProp(trimmed = true)

    /** The name held by this [DbInstitution].!*/
    var zip by xdStringProp(trimmed = true)

    /** The name held by this [DbInstitution].!*/
    var email by xdStringProp(trimmed = true)

    /** The name held by this [DbInstitution].!*/
    var homepage by xdStringProp(trimmed = true)

    /** Flag indicating whether this [DbInstitution]'s metadata should be published. */
    var publish by xdBooleanProp()

    /** List of [DbUser]s that belong to this [DbInstitution]. */
    val users by xdLink0_N(DbUser::institution, onDelete = OnDeletePolicy.CASCADE, onTargetDelete = OnDeletePolicy.CLEAR)
}