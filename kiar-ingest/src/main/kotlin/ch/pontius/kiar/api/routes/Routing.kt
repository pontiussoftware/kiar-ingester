package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.routes.collection.*
import ch.pontius.kiar.api.routes.config.*
import ch.pontius.kiar.api.routes.institution.postSyncInstitutions
import ch.pontius.kiar.api.routes.job.*
import ch.pontius.kiar.api.routes.masterdata.listCantons
import ch.pontius.kiar.api.routes.masterdata.listRightStatements
import ch.pontius.kiar.api.routes.oai.getOaiPmh
import ch.pontius.kiar.api.routes.oai.postOaiPmh
import ch.pontius.kiar.api.routes.session.*
import ch.pontius.kiar.api.routes.user.*
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.oai.OaiServer
import createEntityMapping
import deleteEntityMapping
import deleteInstitution
import getEntityMapping
import getImageForInstitution
import getInstitution
import getListInstitutionNames
import getListInstitutions

import io.javalin.apibuilder.ApiBuilder.*
import jetbrains.exodus.database.TransientEntityStore
import listEntityMappings
import listMappingFormats
import listParsers
import postCreateInstitution
import postUploadImageForInstitution
import putUpdateInstitution
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

        /* Endpoints related to user management. */
        get("users", { ctx -> getListUsers(ctx, store) }, Role.ADMINISTRATOR )
        post("users", { ctx -> postCreateUser(ctx, store) }, Role.ADMINISTRATOR )
        path("users") {
            get("roles", { ctx -> getListRoles(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}", { ctx -> deleteUser(ctx, store) }, Role.ADMINISTRATOR )
            put("{id}",  { ctx -> putUpdateUser(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER  )
        }

        /* Endpoints related to institutions. */
        get("institutions", { ctx -> getListInstitutions(ctx, store) }, Role.ADMINISTRATOR )
        post("institutions", { ctx -> postCreateInstitution(ctx, store) }, Role.ADMINISTRATOR )
        path("institutions") {
            get("name", { ctx -> getListInstitutionNames(ctx, store) }, Role.ADMINISTRATOR )
            post("synchronize", { ctx -> postSyncInstitutions(ctx, store) }, Role.ADMINISTRATOR )
            get("{id}",  { ctx -> getInstitution(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
            put("{id}",  { ctx -> putUpdateInstitution(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER)
            delete("{id}",  { ctx -> deleteInstitution(ctx, store) }, Role.ADMINISTRATOR )
            path("{id}") {
                get("image", { ctx -> getImageForInstitution(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
                post("image", { ctx -> postUploadImageForInstitution(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER)
            }
        }

        /* Endpoints related to collections. */
        get("collections", { ctx -> getListCollections(ctx, store) }, Role.ADMINISTRATOR )
        post("collections", { ctx -> postCreateCollection(ctx, store) }, Role.ADMINISTRATOR )
        path("collections") {
            post("synchronize", { ctx -> postSyncCollections(ctx, store) }, Role.ADMINISTRATOR )
            get("{id}",  { ctx -> getCollection(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
            put("{id}",  { ctx -> putUpdateCollection(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER)
            delete("{id}",  { ctx -> deleteCollection(ctx, store) }, Role.ADMINISTRATOR )
            post("{id}", { ctx -> postUploadImageForCollection(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER)
            path("{id}") {
                get("{name}", { ctx -> getImageForCollection(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
                delete("{name}", { ctx -> deleteImageForCollection(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
            }
        }


        /* Endpoints related to master data. */
        path("masterdata") {
            get("rightstatements", { ctx -> listRightStatements(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            get("cantons", { ctx -> listCantons(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
        }

        /* Endpoints related to jobs. */
        post("jobs", { ctx -> createJob(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER)
        path("jobs") {
            get("active", { ctx -> getActiveJobs(ctx, store, server) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            get("inactive",  { ctx -> getInactiveJobs(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            delete("{id}",  { ctx -> abortJob(ctx, store, server) }, Role.ADMINISTRATOR, Role.MANAGER )
            path("{id}") {
                put("upload",  { ctx -> upload(ctx, store, config) }, Role.ADMINISTRATOR, Role.MANAGER )
                put("schedule",  { ctx -> scheduleJob(ctx, store, server) }, Role.ADMINISTRATOR, Role.MANAGER )
                get("logs",  { ctx -> getJobLogs(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
                delete("logs",  { ctx -> purgeJobLogs(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
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
            get("{id}",  { ctx -> getJobTemplate(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
            put("{id}",  { ctx -> updateJobTemplate(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteJobTemplate(ctx, store, server) }, Role.ADMINISTRATOR )
        }

        /* Endpoint related to Apache Solr configurations. */
        get("solr", { ctx -> listSolrConfigurations(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
        post("solr", { ctx -> createSolrConfig(ctx, store) }, Role.ADMINISTRATOR )
        path("solr") {
            get("formats", { ctx -> listFormats(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
            get("{id}", { ctx -> getSolrConfig(ctx, store) }, Role.ADMINISTRATOR, Role.MANAGER )
            put("{id}", { ctx -> updateSolrConfig(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}", { ctx -> deleteSolrConfig(ctx, store) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to transformers. */
        path("transformers") {
            get("types", { ctx -> listTransformerTypes(ctx, store) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to entity mappings. */
        get("mappings", { ctx -> listEntityMappings(ctx, store) }, Role.ADMINISTRATOR )
        post("mappings", { ctx -> createEntityMapping(ctx, store) }, Role.ADMINISTRATOR )
        path("mappings") {
            get("formats",  { ctx -> listMappingFormats(ctx, store) }, Role.ADMINISTRATOR )
            get("parsers",  { ctx -> listParsers(ctx, store) }, Role.ADMINISTRATOR )
            get("{id}",  { ctx -> getEntityMapping(ctx, store) }, Role.ADMINISTRATOR )
            put("{id}",  { ctx -> updateEntityMapping(ctx, store) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteEntityMapping(ctx, store) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to OAI-PMH. */
        path("{collection}") {
            val oai = OaiServer(store)
            get("oai-pmh",  { ctx -> getOaiPmh(ctx, oai) } )
            post("oai-pmh",  { ctx -> postOaiPmh(ctx, oai) } )
        }
    }
}
