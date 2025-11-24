package ch.pontius.kiar

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.routes.DatabaseAccessManager
import ch.pontius.kiar.api.routes.configureApiRoutes
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.Schema
import ch.pontius.kiar.utilities.KotlinxJsonMapper
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.plugin.DefinitionConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.OpenApiPluginConfiguration
import io.javalin.openapi.plugin.SecurityComponentConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import kotlin.system.exitProcess


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

        /* Start Javalin web-server (if configured). */
        if (config.web) {
            initializeWebserver(config).start(config.webPort)
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
 * Initializes and returns the [Database] based on the provided [Config].
 *
 * @param config The program [Config].
 * @return [Javalin]
 */
private fun initializeWebserver(config: Config) = Javalin.create { c ->
    /* Configure static routes for SPA. */
    c.staticFiles.add{
        it.directory = "html/browser/"
        it.location = Location.CLASSPATH
    }
    c.spaRoot.addFile("/", "html/browser/index.html")

    /* Configure routes. */
    c.router.apiBuilder {
        configureApiRoutes(config)
    }

    /* We use Kotlinx serialization for de-/serialization. */
    c.jsonMapper(KotlinxJsonMapper)

    /* Enable CORS. */
    c.bundledPlugins.enableCors { cors ->
        cors.addRule {
            it.reflectClientOrigin = true // anyHost() has similar implications and might be used in production? I'm not sure how to cope with production and dev here simultaneously
            it.allowCredentials = true
        }
    }

    /* Registers Open API plugin. */
    c.registerPlugin(OpenApiPlugin { openApiConfig: OpenApiPluginConfiguration ->
        openApiConfig
            .withDocumentationPath("/swagger-docs")
            .withDefinitionConfiguration { version: String, openApiDefinition: DefinitionConfiguration ->
                openApiDefinition
                    .withInfo { openApiInfo: OpenApiInfo ->
                        openApiInfo
                            .title("KIAR Dashboard API")
                            .version("1.0.1")
                            .description("API for the KIAR Dashboard.")
                            .contact("API Support", "https://support.kimnet.ch", "support@kimnet.ch")
                    }
                    .withSecurity { openApiSecurity: SecurityComponentConfiguration ->
                        openApiSecurity.withCookieAuth("CookieAuth", "SESSIONID")
                    }
            }
    })

    /* Registers Swagger Plugin. */
    c.registerPlugin(SwaggerPlugin { swaggerConfiguration: SwaggerConfiguration ->
        swaggerConfiguration.documentationPath = "/swagger-docs"
        swaggerConfiguration.uiPath = "/swagger-ui"
    })
}.beforeMatched(DatabaseAccessManager())
.exception(ErrorStatusException::class.java) { e, ctx ->
    ctx.status(e.code).json(ErrorStatus(e.code, e.message))
}.exception(Exception::class.java) { e, ctx ->
    ctx.status(500).json(ErrorStatus(500, "Internal server error: ${e.localizedMessage}"))
}