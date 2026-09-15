package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.UserSession
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.database.institutions.Users
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Configuration for the [RoleAuthorization] plugin.
 */
class RoleAuthorizationConfig {
    /** The set of [Role]s permitted to access the routes this plugin is installed on. */
    var roles: Set<Role> = emptySet()
}

/**
 * A route-scoped plugin that checks a user's eligibility to access certain API resources.
 *
 * Semantics (unchanged from the previous access manager):
 * - No session → 401
 * - Error while loading the user (including an unknown / inactive user) → 500
 * - User's role not among the permitted roles → 403
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
val RoleAuthorization = createRouteScopedPlugin("RoleAuthorization", ::RoleAuthorizationConfig) {
    val permitted = this.pluginConfig.roles
    onCall { call ->
        if (permitted.isEmpty()) {
            return@onCall
        }
        val session = call.sessions.get<UserSession>() ?: throw ErrorStatusException(401, "Unknown user: You cannot access this resource.")
        val user = withContext(Dispatchers.IO) {
            transaction {
                try {
                    Users.getById(session.userId) ?: throw ErrorStatusException(401, "Access denied! Unknown or inactive user.")
                } catch (_: Throwable) {
                    throw ErrorStatusException(500, "Access denied! Error while verifying user.")
                }
            }
        }

        /* Compare user's role to permitted roles. */
        if (!permitted.contains(user.role)) {
            throw ErrorStatusException(403, "Unauthorized user: You cannot access this resource.")
        }
    }
}

/**
 * A transparent [RouteSelector] used to group routes that share the same set of permitted [Role]s.
 */
private class RoleRouteSelector(private val roles: Set<Role>) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation = RouteSelectorEvaluation.Transparent
    override fun toString(): String = "(authorized ${this.roles.joinToString(", ")})"
}

/**
 * Creates a child [Route] whose routes can only be accessed by users having one of the given [Role]s.
 *
 * @param roles The [Role]s permitted to access the routes.
 * @param build The routes to register.
 * @return The created [Route].
 */
fun Route.authorized(vararg roles: Role, build: Route.() -> Unit): Route {
    val route = this.createChild(RoleRouteSelector(roles.toSet()))
    route.install(RoleAuthorization) { this.roles = roles.toSet() }
    route.build()
    return route
}
