package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.routes.users.login
import ch.pontius.kiar.api.routes.users.logout
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
            post("login") { login(it) }
            get("logout") { logout(it) }
        }
    }
}
