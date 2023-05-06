package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.routes.session.SESSION_USER_ID
import ch.pontius.kiar.database.institution.DbUser
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.security.AccessManager
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.util.findById

/**
 * A [AccessManager] implementation that check's a user's eligibility to access certain API resources.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DatabaseAccessManager(private val store: TransientEntityStore): AccessManager {

    override fun manage(handler: Handler, ctx: Context, routeRoles: Set<RouteRole>) = when {
        routeRoles.isEmpty() -> handler.handle(ctx)
        else -> {
            this.store.transactional(true) {
                val userId = ctx.sessionAttribute<String>(SESSION_USER_ID) ?: throw ErrorStatusException(401, "Unknown user: You cannot access this resource.")
                val user = try {
                    DbUser.findById(userId)
                } catch (e: Throwable) {
                    throw ErrorStatusException(401, "Unknown user: You cannot access this resource.")
                }

                /* Compare user's role to permitted roles. */
                if (routeRoles.contains(user.role.toApi())) {
                    handler.handle(ctx)
                } else {
                    throw ErrorStatusException(403, "Unauthorized user: You cannot access this resource.")
                }
            }
        }
    }
}