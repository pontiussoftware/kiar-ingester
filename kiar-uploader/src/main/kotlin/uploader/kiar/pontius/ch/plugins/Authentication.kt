package uploader.kiar.pontius.ch.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

/**
 * Configures HTTP basic authentication for KTOR.
 */
fun Application.configureAuthentication() {
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