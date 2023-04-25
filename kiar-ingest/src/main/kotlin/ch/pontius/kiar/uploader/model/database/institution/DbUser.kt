package ch.pontius.kiar.uploader.model.database.institution

import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.constraints.email
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.simple.alphaNumeric

/**
 * The [DbUser] that can login to the KIAR Uploader Tool
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbUser(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbUser>()

    /** The username of this [DbUser]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true) { alphaNumeric("Username can only consist of digits and numbers.") }

    /** The email of this [DbUser]. Must be unique!*/
    var email by xdRequiredStringProp(unique = true, trimmed = true) { email("A user requires a a proper e-mail address.") }

    /** The password of this [DbUser].*/
    var password by xdRequiredStringProp(trimmed = true)

    /** Flag indicating that this [DbUser] is active (i.e., can log in).*/
    var inactive by xdBooleanProp()

    /** The [DbRole] of this [DbUser]. */
    var role by xdLink1(DbRole)

    /** The [DbInstitution] this [DbUser] belongs to. */
    //var institution by xdLink0_1<DbUser, DbInstitution>(DbInstitution::users, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CASCADE)
}