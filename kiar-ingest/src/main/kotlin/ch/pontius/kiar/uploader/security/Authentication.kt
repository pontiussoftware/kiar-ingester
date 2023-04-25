package ch.pontius.kiar.uploader.security

import io.ktor.server.application.*
import io.ktor.server.auth.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * Configures HTTP basic authentication for KTOR.
 */
fun Application.configureAuthentication(store: TransientEntityStore) {
    install(Authentication) {
        basic("basic-auth") {
            realm = "Access to protected areas of the KIAR uploader."
            validate { credentials ->
                /* TODO: Proper authentication. */
                if (credentials.name == "jetbrains" && credentials.password == "foobar") {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
}