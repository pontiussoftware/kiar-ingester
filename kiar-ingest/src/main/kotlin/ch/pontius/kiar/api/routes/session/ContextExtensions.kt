package ch.pontius.kiar.api.routes.session

import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.database.institution.DbUser
import io.javalin.http.Context
import kotlinx.dnq.util.findById
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
 * A convenience method used to set the currently active user in the session.
 */
fun Context.setUser(user: DbUser) {
    this.sessionAttribute(SESSION_USER_ID, user.xdId)
    this.sessionAttribute(SESSION_USER_NAME, user.name)
}

/**
 * A convenience method used to invalidate the currently active user in the session.
 */
fun Context.invalidateUser() {
    this.sessionAttribute(SESSION_USER_ID, null)
    this.sessionAttribute(SESSION_USER_NAME, null)
}

/**
 * A convenience method used to access the currently active user from the session.
 *
 * Requires an ongoing database transaction.
 *
 * @return [DbUser]
 */
fun Context.currentUser(): DbUser {
    val userID = this.sessionAttribute<String>(SESSION_USER_ID) ?: throw ErrorStatusException(403, "Your are not logged in.")
    try {
        return DbUser.findById(userID)
    } catch (e: Throwable) {
        this.sessionAttribute(SESSION_USER_ID, null)
        this.sessionAttribute(SESSION_USER_NAME, null)
        throw ErrorStatusException(403, "Unable to find user associated with session.")
    }
}