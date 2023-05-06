package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.routes.session.Role
import ch.pontius.kiar.api.routes.session.login
import ch.pontius.kiar.api.routes.session.logout
import ch.pontius.kiar.api.routes.session.status
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
            post("login") { ctx -> login(ctx, store) }
            get("logout", { ctx -> logout(ctx) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
            get("status", { ctx -> status(ctx, store) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
        }
    }
}
