package ch.pontius.kiar.migration.database.institution

import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Users
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.constraints.email
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.simple.alphaNumeric
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.Instant

/**
 * The [DbUser] that can login to the KIAR Uploader Tool
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbUser(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbUser>() {
        fun migrate() {
            all().asSequence().forEach { dbUser ->
                Users.insert {
                    it[name] = dbUser.name
                    it[email] = dbUser.email
                    it[institutionId] = dbUser.institution?.name?.let { Institutions.idByName(it) }
                    it[role] = Role.valueOf(dbUser.role.description)
                    it[password] = dbUser.password
                    it[inactive] = dbUser.inactive
                    it[created] = dbUser.createdAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                    it[modified] = dbUser.changedAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                }
            }
        }
    }

    /** The username of this [DbUser]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true) { alphaNumeric("Username can only consist of digits and numbers.") }

    /** The email of this [DbUser]. Must be unique!*/
    var email by xdStringProp(trimmed = true) { email("A user requires a a proper e-mail address.") }

    /** The password of this [DbUser].*/
    var password by xdRequiredStringProp(trimmed = true)

    /** Flag indicating that this [DbUser] is active (i.e., can log in).*/
    var inactive by xdBooleanProp()

    /** The [DbRole] of this [DbUser]. */
    var role by xdLink1(DbRole)

    /** The date and time this [DbUser] was created. */
    var createdAt by xdDateTimeProp()

    /** The date and time this [DbUser] was last changed. */
    var changedAt by xdDateTimeProp()

    /** The [DbInstitution] this [DbUser] belongs to. */
    var institution: DbInstitution? by xdLink0_1(DbInstitution::users, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CASCADE)
}