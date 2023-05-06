package ch.pontius.kiar.api.model.session

import ch.pontius.kiar.database.institution.DbRole
import io.javalin.security.RouteRole

/**
 * The [Role] a user can have.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Role : RouteRole {
    ADMINISTRATOR, MANAGER, VIEWER;

    /**
     * Converts hit [Role] to a [DbRole]. Requires an active transaction.
     *
     * @return [DbRole]
     */
    fun toDb(): DbRole = when(this) {
        ADMINISTRATOR -> DbRole.ADMINISTRATOR
        MANAGER -> DbRole.MANAGER
        VIEWER -> DbRole.VIEWER
    }
}