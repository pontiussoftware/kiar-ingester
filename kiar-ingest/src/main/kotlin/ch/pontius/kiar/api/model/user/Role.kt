package ch.pontius.kiar.api.model.user

import io.javalin.security.RouteRole

/**
 * The [Role] a [User] can have.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Role: RouteRole {
    ADMINISTRATOR,
    MANAGER,
    VIEWER;
}