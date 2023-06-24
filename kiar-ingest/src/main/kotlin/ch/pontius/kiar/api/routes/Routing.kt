package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.model.session.Role
import ch.pontius.kiar.api.routes.config.*
import ch.pontius.kiar.api.routes.session.*
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
fun configureApiRoutes(store: TransientEntityStore) {
    /** Path to API related functionality. */
    path("api") {
        /** All paths related to session, login and logout handling. */
        path("session") {
            post("login") { ctx -> login(ctx, store) }
            get("logout", { ctx -> logout(ctx) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
            get("status", { ctx -> status(ctx, store) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
            get("user", { ctx -> getUser(ctx, store) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
            post("user", { ctx -> updateUser(ctx, store) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER )
        }

        /* Endpoints related to participants. */
        get("participants", { ctx -> listParticipants(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
        post("participants", { ctx -> createParticipants(ctx, store) }, Role.ADMINISTRATOR )
        path("participants") {
            delete("{id}",  { ctx -> deleteParticipants(ctx, store) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to job templates. */
        get("templates", { ctx -> listJobTemplates(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
        post("templates", { ctx -> createJobTemplate(ctx, store) }, Role.ADMINISTRATOR )
        path("templates") {
            get("types",  { ctx -> listJobTemplateTypes(ctx, store) }, Role.ADMINISTRATOR )
            put("{id}",  { ctx -> updateJobTemplate(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteJobTemplate(ctx, store) }, Role.ADMINISTRATOR )
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
