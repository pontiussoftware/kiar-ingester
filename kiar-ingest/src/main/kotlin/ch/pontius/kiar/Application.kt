package ch.pontius.kiar

import ch.pontius.kiar.api.routes.configureApiRoutes
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.cli.Cli
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.config.jobs.DbJobType
import ch.pontius.kiar.database.config.mapping.*
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbCollectionType
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.database.config.transformers.DbTransformer
import ch.pontius.kiar.database.config.transformers.DbTransformerParameter
import ch.pontius.kiar.database.config.transformers.DbTransformerType
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobStatus
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.database.institution.DbParticipant
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.database.institution.DbUser
import ch.pontius.kiar.utilities.KotlinxJsonMapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.json.JavalinJackson
import io.javalin.openapi.CookieAuth
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.OpenApiPluginConfiguration
import io.javalin.openapi.plugin.SecurityComponentConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdModel
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

/** The [TransientEntityStore] representing this instance's database. */
var DB: TransientEntityStore? = null

/** The [IngesterServer] running for this application instance. */
var SERVER: IngesterServer? = null

/**
 * Entry point for KIAR Tools.
 */
fun main(args: Array<String>) {
    /* Try to start Cottontail DB */
    try {
        val config: Config = loadConfig(args.firstOrNull() ?: "./config.json")
        System.setProperty("log4j.saveDirectory", config.logPath.toString()) /* Set log path for Log4j2. */

        /* Initializes the embedded Xodus database. */
        val database = initializeDatabase(config)

        /* Initializes the IngestServer. */
        SERVER = IngesterServer(database, config)

        /* Start Javalin web-server (if configured). */
        if (config.web) {
            initializeWebserver().start(config.webPort)
        }

        /* Start CLI (if configured). */
        if (config.cli) {
            Cli(config, SERVER!!).loop()
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
 * Initializes and returns the [TransientEntityStore] based on the provided [Config].
 *
 * @return [TransientEntityStore]
 */
private fun initializeDatabase(config: Config): TransientEntityStore {
    val store = StaticStoreContainer.init(dbFolder = config.dbPath.toFile(), entityStoreName = "kiar-db")
    XdModel.registerNodes(
        DbSolr,
        DbCollection,
        DbCollectionType,
        DbJobTemplate,
        DbJobType,
        DbEntityMapping,
        DbAttributeMapping,
        DbAttributeMappingParameters,
        DbFormat,
        DbParser,
        DbTransformer,
        DbTransformerParameter,
        DbTransformerType,
        DbParticipant,
        DbJob,
        DbJobStatus,
        DbInstitution,
        DbRole,
        DbUser
    )
    initMetaData(XdModel.hierarchy, store)
    return store;
}

/**
 * Initializes and returns the [TransientEntityStore] based on the provided [Config].
 *
 * @return [TransientEntityStore]
 */
private fun initializeWebserver() = Javalin.create { config ->
    config.staticFiles.add{
        it.directory = "html"
        it.location = Location.CLASSPATH
    }
    config.spaRoot.addFile("/", "html/index.html")
    config.plugins.enableCors { cors ->
        cors.add {
            it.reflectClientOrigin = true // anyHost() has similar implications and might be used in production? I'm not sure how to cope with production and dev here simultaneously
            it.allowCredentials = true
        }
    }

    /* We use Kotlinx serialization for de-/serialization. */
    config.jsonMapper(KotlinxJsonMapper)

    /* Registers Open API plugin. */
    config.plugins.register(
        OpenApiPlugin(
            OpenApiPluginConfiguration()
                .withDocumentationPath("/swagger-docs")
                .withDefinitionConfiguration { _, u ->
                    u.withOpenApiInfo { t ->
                        t.title = "KIAR Dashboard API"
                        t.version = "1.0.0"
                        t.description = "API for the KIAR Dashboard."
                    }
                    u.withSecurity(
                        SecurityComponentConfiguration().withSecurityScheme("CookieAuth", CookieAuth("SESSIONID"))
                    )
                }
        )
    )

    /* Registers Swagger Plugin. */
    config.plugins.register(
        SwaggerPlugin(
            SwaggerConfiguration().apply {
                this.version = "4.10.3"
                this.documentationPath = "/swagger-docs"
                this.uiPath = "/swagger-ui"
            }
        )
    )
}.routes {
    configureApiRoutes(DB ?: throw IllegalStateException("The database has not been properly initialized. This is a programmer's error!"))
}
