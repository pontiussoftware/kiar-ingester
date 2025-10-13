package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.utilities.extensions.SESSION_USER_ID
import ch.pontius.kiar.database.institutions.Users
import io.javalin.http.Context
import io.javalin.http.Handler
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * A [Handler] implementation that check's a user's eligibility to access certain API resources.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class DatabaseAccessManager(): Handler {
    override fun handle(ctx: Context) {
        val routeRoles = ctx.routeRoles()
        if (routeRoles.isEmpty()) {
            return
        }
        val user = transaction {
            val userId = ctx.sessionAttribute<Int>(SESSION_USER_ID) ?: throw ErrorStatusException(401, "Unknown user: You cannot access this resource.")
            try {
                Users.getById(userId) ?: throw ErrorStatusException(401, "Access denied! Unknown or inactive user.")
            } catch (e: Throwable) {
                throw ErrorStatusException(500, "Access denied! Error while verifying user.")
            }
        }

        /* Compare user's role to permitted roles. */
        if (!routeRoles.contains(user.role)) {
            throw ErrorStatusException(403, "Unauthorized user: You cannot access this resource.")
        }
    }
}