package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.model.session.Role
import ch.pontius.kiar.api.routes.config.*
import ch.pontius.kiar.api.routes.job.*
import ch.pontius.kiar.api.routes.session.*
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.ingester.IngesterServer
import createEntityMapping
import deleteEntityMapping
import getEntityMapping
import io.javalin.apibuilder.ApiBuilder.*
import jetbrains.exodus.database.TransientEntityStore
import listEntityMappings
import listParsers
import updateEntityMapping

/**
 * Configures all the API routes.
 *
 * @param store The [TransientEntityStore] used for persistence.
 */
fun configureApiRoutes(store: TransientEntityStore, server: IngesterServer, config: Config) {
    /** Path to API related functionality. */
    path("api") {
        /** All paths related to session, login and logout handling. */
        path("session") {
            post("login") { ctx -> login(ctx, store) }
            get("logout", { ctx -> logout(ctx) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
            get("status", { ctx -> status(ctx, store) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
            get("user", { ctx -> getUser(ctx, store) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
            put("user", { ctx -> updateUser(ctx, store) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
        }

        /* Endpoints related to jobs. */
        post("jobs", { ctx -> createJob(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER)
        path("jobs") {
            get("active", { ctx -> getActiveJobs(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            get("inactive",  { ctx -> getInactiveJobs(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            delete("{id}",  { ctx -> getInactiveJobs(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
            path("{id}") {
                put("upload",  { ctx -> uploadKiar(ctx, store, config) }, Role.ADMINISTRATOR, Role.MANAGER )
                put("schedule",  { ctx -> scheduleJob(ctx, store, server) }, Role.ADMINISTRATOR, Role.MANAGER )
            }
        }

        /* Endpoints related to participants. */
        get("participants", { ctx -> listParticipants(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
        path("participants") {
            post("{name}", { ctx -> createParticipants(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteParticipants(ctx, store) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to job templates. */
        get("templates", { ctx -> listJobTemplates(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
        post("templates", { ctx -> createJobTemplate(ctx, store, server) }, Role.ADMINISTRATOR )
        path("templates") {
            get("types",  { ctx -> listJobTemplateTypes(ctx, store) }, Role.ADMINISTRATOR )
            put("{id}",  { ctx -> updateJobTemplate(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteJobTemplate(ctx, store, server) }, Role.ADMINISTRATOR )
        }

        /* Endpoint related to Apache Solr configurations. */
        get("solr", { ctx -> listSolrConfigurations(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
        post("solr", { ctx -> createSolrConfig(ctx, store) }, Role.ADMINISTRATOR )
        path("solr") {
            put("{id}",  { ctx -> updateSolrConfig(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteSolrConfig(ctx, store) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to transformers. */
        path("transformers") {
            get("types", { ctx -> listTransformerTypes(ctx, store) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to entity mappings. */
        get("mappings", { ctx -> listEntityMappings(ctx, store) }, Role.ADMINISTRATOR )
        post("mappings", { ctx -> createEntityMapping(ctx, store) }, Role.ADMINISTRATOR )
        path("mappings") {
            get("parsers",  { ctx -> listParsers(ctx, store) }, Role.ADMINISTRATOR )
            get("{id}",  { ctx -> getEntityMapping(ctx, store) }, Role.ADMINISTRATOR )
            put("{id}",  { ctx -> updateEntityMapping(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteEntityMapping(ctx, store) }, Role.ADMINISTRATOR )
        }
    }
}
