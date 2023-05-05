package ch.pontius.kiar.api.routes.session

import io.javalin.security.RouteRole

/**
 * The [Role] a user can have.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Role : RouteRole {
    ANYONE,
    ADMINISTRATOR,
    MANAGER,
    VIEWER
}