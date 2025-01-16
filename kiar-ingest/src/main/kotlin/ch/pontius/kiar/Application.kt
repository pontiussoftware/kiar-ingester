package ch.pontius.kiar

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.routes.DatabaseAccessManager
import ch.pontius.kiar.api.routes.configureApiRoutes
import ch.pontius.kiar.api.routes.session.SALT
import ch.pontius.kiar.cli.Cli
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.collection.DbObjectCollection
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.config.jobs.DbJobType
import ch.pontius.kiar.database.config.mapping.*
import ch.pontius.kiar.database.config.solr.*
import ch.pontius.kiar.database.config.transformers.DbTransformer
import ch.pontius.kiar.database.config.transformers.DbTransformerParameter
import ch.pontius.kiar.database.config.transformers.DbTransformerType
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.database.institution.DbParticipant
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.database.institution.DbUser
import ch.pontius.kiar.database.job.*
import ch.pontius.kiar.database.masterdata.DbCanton
import ch.pontius.kiar.database.masterdata.DbRightStatement
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.tasks.PurgeJobLogTask
import ch.pontius.kiar.tasks.RemoveInputFilesTask
import ch.pontius.kiar.utilities.KotlinxJsonMapper
import ch.pontius.kiar.utilities.generatePassword
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.openapi.*
import io.javalin.openapi.plugin.*
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdModel
import kotlinx.dnq.query.*
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.system.exitProcess


/**
 * Entry point for KIAR Tools.
 */
fun main(args: Array<String>) {
    /* Try to start Cottontail DB */
    try {
        val config: Config = loadConfig(args.firstOrNull() ?: "./config.json")
        System.setProperty("log4j.saveDirectory", config.logPath.toString()) /* Set log path for Log4j2. */

        /* Initializes the embedded Xodus database. */
        val store = initializeDatabase(config)

        /* Initializes the IngestServer. */
        val server = IngesterServer(store, config)

        /* Start Javalin web-server (if configured). */
        if (config.web) {
            initializeWebserver(store, server, config).start(config.webPort)
        }

        /* Schedule timer tasks. */
        val timer = Timer("Task scheduler")
        timer.scheduleAtFixedRate(PurgeJobLogTask(store, config), 5000, 86400000)
        timer.scheduleAtFixedRate(RemoveInputFilesTask(store, config), 5000, 86400000)

        /* Start CLI (if configured). */
        if (config.cli) {
            Cli(config, server, store).loop()
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
        DbJobSource,
        DbEntityMapping,
        DbAttributeMapping,
        DbAttributeMappingParameters,
        DbRightStatement,
        DbCanton,
        DbFormat,
        DbParser,
        DbTransformer,
        DbTransformerParameter,
        DbTransformerType,
        DbImageFormat,
        DbImageDeployment,
        DbParticipant,
        DbJob,
        DbJobLog,
        DbJobLogLevel,
        DbJobLogContext,
        DbJobStatus,
        DbInstitution,
        DbObjectCollection,
        DbRole,
        DbUser
    )
    initMetaData(XdModel.hierarchy, store)

    /* Perform basic setup if needed. */
    checkAndSetup(store, config)

    return store
}

/**
 * Checks for an empty database and performs basic setup if needed.
 *
 * @param store [TransientEntityStore]
 * @param config The [Config]
 */
private fun checkAndSetup(store: TransientEntityStore, config: Config) = store.transactional {
    if (DbUser.all().size() == 0) {
        println("Empty database encountered... starting setup.")
        val pw = generatePassword(10)
        DbUser.new {
            name = "admin"
            role = DbRole.ADMINISTRATOR
            password = BCrypt.hashpw(pw, SALT)
            inactive = false
        }
        println("Generated a new user 'admin' with password '$pw'.")
    }
}

/**
 * Initializes and returns the [TransientEntityStore] based on the provided [Config].
 *
 * @return [TransientEntityStore]
 */
private fun initializeWebserver(store: TransientEntityStore, server: IngesterServer, config: Config) = Javalin.create { c ->
    /* Configure static routes for SPA. */
    c.staticFiles.add{
        it.directory = "html"
        it.location = Location.CLASSPATH
    }
    c.spaRoot.addFile("/", "html/index.html")

    /* Configure routes. */
    c.router.apiBuilder() {
        configureApiRoutes(store, server, config)
    }

    /* We use Kotlinx serialization for de-/serialization. */
    c.jsonMapper(KotlinxJsonMapper)

    /* Enable CORS. */
    c.bundledPlugins.enableCors { cors ->
        cors.addRule() {
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
                            .version("1.0.0")
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
}.beforeMatched(DatabaseAccessManager(store))
.exception(ErrorStatusException::class.java) { e, ctx ->
    ctx.status(e.code).json(ErrorStatus(e.code, e.message))
}.exception(Exception::class.java) { e, ctx ->
    ctx.status(500).json(ErrorStatus(500, "Internal server error: ${e.localizedMessage}"))
}