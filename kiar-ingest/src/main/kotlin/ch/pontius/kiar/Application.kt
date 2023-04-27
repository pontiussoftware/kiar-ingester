package ch.pontius.kiar

import ch.pontius.kiar.api.routes.configureRoutes
import ch.pontius.kiar.api.security.configureAuthentication
import ch.pontius.kiar.api.security.configureSecurity
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.cli.Cli
import ch.pontius.kiar.config.Config
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

/** The [TransientEntityStore] representing this instance's database. */
var DB: TransientEntityStore? = null

/** The [IngesterServer] running for this application instance. */
var SERVER: IngesterServer? = null

/**
 * Entry point for [IngesterServer].
 */
fun main(args: Array<String>) {
    /* Try to start Cottontail DB */
    try {
        val config: Config = loadConfig(args.firstOrNull() ?: "./config.json")
        SERVER = IngesterServer(config)

        /* Initializes the embedded Xodus database. */
        //XdModel.registerNodes(DbJob, DbTaskStatus, DbInstitution, DbRole, DbUser)
        //initMetaData(XdModel.hierarchy, store)

        /* Start Ktor web-server (if configured). */
        if (config.web) {
            embeddedServer(Netty, port = config.webPort, host = "0.0.0.0", module = Application::module).start(wait = true)
        }

        /* Start CLI (if configured). */
        if (config.cli) {
            Cli(SERVER!!).loop()
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
 * Configures the Ktor application
 */
private fun Application.module() {
    /* Configures security and authentication. */
    configureSecurity()
    configureAuthentication(DB ?: throw IllegalStateException("Database has not been initialized. Program is terminated!"))

    /* Configures routes for API. */
    configureRoutes(DB ?: throw IllegalStateException("Database has not been initialized. Program is terminated!"))
}
