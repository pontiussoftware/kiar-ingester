package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.routes.session.Role
import ch.pontius.kiar.api.routes.session.login
import io.javalin.apibuilder.ApiBuilder.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * Configures all the API routes.
 *
 * @param store The [TransientEntityStore] used for persistence.
 */
fun configureApiRoutes(store: TransientEntityStore) {
    /** Path to API related functionality. */
    path("api") {
        /** All paths related to session, login and logout handling. */
        path("session") {
            post("login", { ctx -> login(ctx, store) }, Role.ANYONE)
            get("logout", { ctx -> login(ctx, store) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
        }
    }
}
