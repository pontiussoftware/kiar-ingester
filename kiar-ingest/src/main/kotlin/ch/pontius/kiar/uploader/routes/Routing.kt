package ch.pontius.kiar.uploader.routes

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import ch.pontius.kiar.uploader.routes.kiar.kiarUploadRoute
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import jetbrains.exodus.database.TransientEntityStore

fun Application.configureRoutes(store: TransientEntityStore) {
    /* Install JSON serialization. */
    install(ContentNegotiation) {
        json()
    }

    /* Start configuration of routes. */
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        /**
         * Route for API calls.
         */
        route("api") {
            /**
             * Public area of the KIAR Uploader API.
             */


            /**
             * Protected area of the KIAR Uploader API.
             */
            authenticate("basic-auth") {
                kiarUploadRoute()
            }
        }
    }
}
