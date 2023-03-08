package uploader.kiar.pontius.ch.routes

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import uploader.kiar.pontius.ch.routes.kiar.kiarUploadRoute

fun Application.configureRouting() {
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
