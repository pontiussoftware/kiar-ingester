package ch.pontius.kiar.utilities.extensions

import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.database.institutions.Users
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import org.mindrot.jbcrypt.BCrypt

/** Name of the session constant used to store the user ID. */
const val SESSION_USER_ID = "USER_ID"

/** Name of the session constant used to store the user ID. */
const val SESSION_USER_NAME = "USER_NAME"

/** The minimum length of a password. */
const val MIN_LENGTH_PASSWORD = 8

/** The minimum length of a username. */
const val MIN_LENGTH_USERNAME = 5

/** The salt used for password encryption. */
val SALT: String = BCrypt.gensalt()

/**
 * Parses the [Context]s request body as type [T] and throws an [ErrorStatusException] if parsing fails.
 *
 * @return [T]
 */
inline fun <reified T> Context.parseBodyOrThrow() = try {
    this.bodyAsClass(T::class.java)
} catch (_: BadRequestResponse) {
    throw ErrorStatusException(400, "Failed to parse request body; malformed request.")
}

/**
 * A convenience method used to set the currently active [User] in the session.
 */
fun Context.setUser(user: User) {
    this.sessionAttribute(SESSION_USER_ID, user.id!!)
    this.sessionAttribute(SESSION_USER_NAME, user.username)
}

/**
 * A convenience method used to invalidate the currently active [User] in the session.
 */
fun Context.invalidateUser() {
    this.sessionAttribute(SESSION_USER_ID, null)
    this.sessionAttribute(SESSION_USER_NAME, null)
}

/**
 * A convenience method used to access the currently active [User] from the session.
 *
 * Requires an ongoing database transaction.
 *
 * @return [User]
 */
fun Context.currentUser(): User {
    val userId = this.sessionAttribute<Int>(SESSION_USER_ID) ?: throw ErrorStatusException(403, "Your are not logged in.")
    try {
        return Users.getById(userId) ?: throw ErrorStatusException(403, "Unable to find user associated with session.")
    } catch (e: Throwable) {
        this.sessionAttribute(SESSION_USER_ID, null)
        this.sessionAttribute(SESSION_USER_NAME, null)
        throw e
    }
}