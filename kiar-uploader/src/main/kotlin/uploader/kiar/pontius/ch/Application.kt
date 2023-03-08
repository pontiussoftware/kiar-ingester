package uploader.kiar.pontius.ch

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import uploader.kiar.pontius.ch.plugins.*
import uploader.kiar.pontius.ch.routes.configureRouting

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
    .start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureRouting()
}
