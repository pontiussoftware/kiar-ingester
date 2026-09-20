package ch.pontius.kiar

import ch.pontius.kiar.api.UserSession
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.openapi.KiarSchemaInference
import ch.pontius.kiar.api.openapi.serializeOpenApiDoc
import ch.pontius.kiar.api.routes.configureApiRoutes
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.Schema
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.servers.oai.OaiServer
import ch.pontius.kiar.servers.sru.SruServer
import ch.pontius.kiar.utilities.CaffeineSessionStorage
import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.server.sessions.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.time.Duration
import kotlin.system.exitProcess

/** The name of the session cookie. */
const val SESSION_COOKIE = "SESSIONID"

/** The name of the OpenAPI security scheme describing the session cookie. */
const val SECURITY_SCHEME = "CookieAuth"

/**
 * Entry point for KIAR Tools.
 */
fun main(args: Array<String>) {
    /* Try to start Cottontail DB */
    try {
        val config: Config = loadConfig(args.firstOrNull() ?: "./config.json")
        System.setProperty("log4j.saveDirectory", config.logPath.toString()) /* Set log path for Log4j2. */

        /* Initializes the SQLite database and make it default. */
        val database = Database.connect("jdbc:sqlite:${config.dbPath}?foreign_keys=on;", driver = "org.sqlite.JDBC")
        TransactionManager.defaultDatabase = database
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        /* Check and initialize the schema. */
        if (!Schema.check(database)) {
            println("Initializing database schema.")
            Schema.initialize(database)
        }

        /* Start Ktor web-server (if configured). */
        if (config.web) {
            embeddedServer(Netty, port = config.webPort) { kiar(config) }.start(wait = true)
        }
    } catch (e: Throwable) {
        System.err.println("Failed to start IngesterServer due to error:")
        System.err.println(e.printStackTrace())
        exitProcess(1)
    }
}

/**
 * Tries to load (i.e.n read and parse) the [Config] from the path specified and creates a default [Config] file if none exists.
 *
 * @throws FileNotFoundException In case the specified path is not a regular file
 * @throws RuntimeException In case an error occurs during parsing / reading
 * @return The parsed [Config] ready to be used.
 */
private fun loadConfig(path: String): Config {
    val configPath = Paths.get(path)
    try {
        if (!Files.isRegularFile(configPath)) {
            System.err.println("No IngesterServer config exists under $configPath; trying to create default config!")
            exitProcess(1)
        } else {
            return Files.newBufferedReader(configPath).use {
                return@use Json.decodeFromString(Config.serializer(), it.readText())
            }
        }
    } catch (e: Throwable) {
        System.err.println("Could not load IngesterServer configuration file under $configPath.IngesterServer will shutdown!")
        e.printStackTrace()
        exitProcess(1)
    }
}

/**
 * The Ktor [Application] module for the KIAR Dashboard API and SPA based on the provided [Config].
 *
 * @param config The program [Config].
 */
@OptIn(ExperimentalKtorApi::class)
fun Application.kiar(config: Config) {
    /* Initializes the IngestServer and the publication servers. */
    val server = IngesterServer(config)
    val oaiServer = OaiServer()
    val sruServer = SruServer()

    /* We use Kotlinx serialization for de-/serialization. */
    install(ContentNegotiation) {
        json(Json { encodeDefaults = true })
    }

    /* Server-side sessions identified by an opaque cookie. */
    install(Sessions) {
        cookie<UserSession>(SESSION_COOKIE, CaffeineSessionStorage(Duration.ofMinutes(30))) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = config.secureCookies
            cookie.maxAgeInSeconds = null /* Session cookie (no Max-Age); the server-side session expires after 30 minutes of inactivity. */
            cookie.encoding = CookieEncoding.RAW
            cookie.extensions["SameSite"] = "Lax"
        }
    }

    /* Security-related response headers. */
    install(DefaultHeaders) {
        header("X-Frame-Options", "DENY")
        header("X-Content-Type-Options", "nosniff")
        header("Referrer-Policy", "same-origin")
    }

    /* CORS is only needed for cross-origin development setups; the SPA itself is served from the same origin as the API. */
    if (config.allowedOrigins.isNotEmpty()) {
        install(CORS) {
            for (origin in config.allowedOrigins) {
                val schemeSeparator = origin.indexOf("://")
                if (schemeSeparator > 0) {
                    allowHost(origin.substring(schemeSeparator + 3), schemes = listOf(origin.substring(0, schemeSeparator)))
                } else {
                    allowHost(origin, schemes = listOf("http", "https"))
                }
            }
            allowCredentials = true
            allowNonSimpleContentTypes = true
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowMethod(HttpMethod.Options)
        }
    }

    /* Map exceptions to ErrorStatus JSON. */
    install(StatusPages) {
        exception<ErrorStatusException> { call, e ->
            call.respond(HttpStatusCode.fromValue(e.code), ErrorStatus(e.code, e.message))
        }
        exception<Throwable> { call, e ->
            call.respond(HttpStatusCode.InternalServerError, ErrorStatus(500, "Internal server error: ${e.localizedMessage}"))
        }
    }

    /* Configure routes. */
    routing {
        /* API routes. */
        val api: Route = configureApiRoutes(config, server, oaiServer, sruServer)

        /* OpenAPI document generated from the API routes. */
        val source = OpenApiDocSource.Routing(
            contentType = ContentType.Application.Json,
            schemaInference = KiarSchemaInference,
            securitySchemes = { mapOf(SECURITY_SCHEME to ReferenceOr.Value(ApiKeySecurityScheme(name = SESSION_COOKIE, `in` = SecuritySchemeIn.COOKIE))) },
            serializeModel = ::serializeOpenApiDoc,
            routes = { api.descendants() }
        )
        val info = OpenApiInfo(
            title = "KIAR Dashboard API",
            version = "1.0.1",
            description = "API for the KIAR Dashboard.",
            contact = OpenApiInfo.Contact(name = "API Support", url = "https://support.kimnet.ch", email = "support@kimnet.ch")
        )
        val document = this@kiar.async(start = CoroutineStart.LAZY) {
            source.read(this@kiar, OpenApiDoc(info = info))
        }
        get("/swagger-docs") {
            val doc = document.await()
            call.respondText(doc.content, doc.contentType)
        }.hide()

        /* Swagger UI. */
        swaggerUI("/swagger-ui") {
            this.info = info
            this.source = source
            this.remotePath = "swagger-docs.json"
        }

        /* Static routes for SPA. */
        singlePageApplication {
            useResources = true
            filesPath = "html/browser"
            defaultPage = "index.html"
        }
    }
}
