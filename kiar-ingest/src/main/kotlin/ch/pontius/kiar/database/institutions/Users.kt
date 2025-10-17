package ch.pontius.kiar.database.institutions

import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.api.model.user.UserId
import ch.pontius.kiar.database.institutions.Institutions.toInstitution
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * A [Table] that holds information about [Users] that have access to KIAR.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Users : IntIdTable("users") {
    /** Reference to an [Institutions] entry this [Users] entry belongs to. Can be null! */
    val institutionId = reference("institution_id", Institutions, ReferenceOption.SET_NULL).nullable()

    /** The name of the [Users] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** The e-mail of the [Users] entry. */
    val email = varchar("email", 255).nullable()

    /** The password of the [Users] entry. */
    val password = varchar("password", 255).uniqueIndex()

    /** Flag indicating, that a [Users] entry has been inactivated. */
    val inactive = bool("inactive").default(false)

    /** The [Role] of a [Users] entry. */
    val role = enumerationByName("role", 16,Role::class)

    /** Timestamp of creation of the [Participants] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of change of the [Participants] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /**
     * Obtains a [Users] [id] by its [name].
     *
     * @param name The name to lookup
     * @return [Users] [id] or null, if no entry exists.
     */
    fun idByName(name: String) = Users.select(id).where { Users.name eq name }.map { it[id] }.firstOrNull()

    /**
     * Returns an active [User] by its ID.
     *
     * @param id The [User]'s [UserId].
     * @param inactive If true, inactive [Users] will be returned as well.
     */
    fun getById(id: UserId, inactive: Boolean = false) = transaction {
        val query = (Users leftJoin Institutions leftJoin Participants).selectAll().where { (Users.id eq id) }
        if (!inactive) {
            query.andWhere { Users.inactive eq false }
        }
        query.map { it.toUser() }
        .firstOrNull()
    }

    /**
     * Converts a [ResultRow] to a [User] entry.
     *
     * @return [User]
     */
     fun ResultRow.toUser() = User(
        id = this[id].value,
        username = this[name],
        password = this[password],
        email = this[email],
        active = !this[inactive],
        role = this[role],
        institution = this.getOrNull(Institutions.id)?.let { this.toInstitution() },
        createdAt = this[created].toEpochMilli(),
        changedAt = this[modified].toEpochMilli()
     )
}