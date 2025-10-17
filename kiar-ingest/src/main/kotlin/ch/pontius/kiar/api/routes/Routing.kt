package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.routes.collection.*
import ch.pontius.kiar.api.routes.config.*
import ch.pontius.kiar.api.routes.institution.*
import ch.pontius.kiar.api.routes.job.*
import ch.pontius.kiar.api.routes.masterdata.*
import ch.pontius.kiar.api.routes.oai.getOaiPmh
import ch.pontius.kiar.api.routes.oai.postOaiPmh
import ch.pontius.kiar.api.routes.session.*
import ch.pontius.kiar.api.routes.user.*
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.oai.OaiServer
import createEntityMapping
import deleteEntityMapping
import getEntityMapping
import io.javalin.apibuilder.ApiBuilder.*
import listEntityMappings
import updateEntityMapping

/**
 * Configures all the API routes.
 *
 * @param server The [IngesterServer] instance.
 * @param config The program [Config].
 */
fun configureApiRoutes(server: IngesterServer, config: Config) {
    /** Path to API related functionality. */
    path("api") {
        /** All paths related to session, login and logout handling. */
        path("session") {
            post("login") { ctx -> login(ctx) }
            get("logout", { ctx -> logout(ctx) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER)
            get("status", { ctx -> status(ctx) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER)
            get("user", { ctx -> getUser(ctx) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER)
            put("user", { ctx -> updateUser(ctx) }, Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER)
        }

        /* Endpoints related to user management. */
        get("users", { ctx -> getListUsers(ctx) }, Role.ADMINISTRATOR)
        post("users", { ctx -> postCreateUser(ctx) }, Role.ADMINISTRATOR)
        path("users") {
            get("roles", { ctx -> getListRoles(ctx) }, Role.ADMINISTRATOR)
            delete("{id}", { ctx -> deleteUser(ctx) }, Role.ADMINISTRATOR)
            put("{id}", { ctx -> putUpdateUser(ctx) }, Role.ADMINISTRATOR, Role.MANAGER)
        }

        /* Endpoints related to institutions. */
        get("institutions", { ctx -> getListInstitutions(ctx) }, Role.ADMINISTRATOR)
        post("institutions", { ctx -> postCreateInstitution(ctx) }, Role.ADMINISTRATOR)
        path("institutions") {
            get("name", { ctx -> getListInstitutionNames(ctx) }, Role.ADMINISTRATOR)
            post("synchronize", { ctx -> postSyncInstitutions(ctx) }, Role.ADMINISTRATOR)
            get("{id}", { ctx -> getInstitution(ctx) }, Role.ADMINISTRATOR, Role.MANAGER)
            put("{id}", { ctx -> putUpdateInstitution(ctx) }, Role.ADMINISTRATOR, Role.MANAGER)
            delete("{id}", { ctx -> deleteInstitution(ctx) }, Role.ADMINISTRATOR)
            path("{id}") {
                get("image", { ctx -> getImageForInstitution(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
                post("image", { ctx -> postUploadImageForInstitution(ctx) }, Role.ADMINISTRATOR, Role.MANAGER)
            }
        }

        /* Endpoints related to collections. */
        get("collections", { ctx -> getListCollections(ctx) }, Role.ADMINISTRATOR)
        post("collections", { ctx -> postCreateCollection(ctx) }, Role.ADMINISTRATOR)
        path("collections") {
            post("synchronize", { ctx -> postSyncCollections(ctx) }, Role.ADMINISTRATOR)
            get("{id}", { ctx -> getCollection(ctx) }, Role.ADMINISTRATOR, Role.MANAGER)
            put("{id}", { ctx -> putUpdateCollection(ctx) }, Role.ADMINISTRATOR, Role.MANAGER)
            delete("{id}", { ctx -> deleteCollection(ctx) }, Role.ADMINISTRATOR)
            post("{id}", { ctx -> postUploadImageForCollection(ctx) }, Role.ADMINISTRATOR, Role.MANAGER)
            path("{id}") {
                get("{name}", { ctx -> getImageForCollection(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
                delete(
                    "{name}",
                    { ctx -> deleteImageForCollection(ctx) },
                    Role.ADMINISTRATOR,
                    Role.MANAGER,
                    Role.VIEWER
                )
            }
        }

        /* Endpoints related to master data. */
        path("masterdata") {
            get("rightstatements", { ctx -> listRightStatements(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
            get("cantons", { ctx -> listCantons(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
            get("transformers", { ctx -> listTransformerTypes(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
            get("parsers", { ctx -> listParsers(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER)
            get("image-formats",  { ctx -> listImageFormats(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            get("mapping-formats",  { ctx -> listMappingFormats(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            get("job-types",  { ctx -> listJobTemplateTypes(ctx) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )

        }

        /* Endpoints related to jobs. */
        post("jobs", { ctx -> createJob(ctx) }, Role.ADMINISTRATOR, Role.MANAGER)
        path("jobs") {
            get("active", { ctx -> getActiveJobs(ctx, server) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            get("inactive",  { ctx -> getInactiveJobs(ctx, server) }, Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER )
            delete("{id}",  { ctx -> abortJob(ctx, server) }, Role.ADMINISTRATOR, Role.MANAGER )
            path("{id}") {
                put("upload",  { ctx -> upload(ctx, config) }, Role.ADMINISTRATOR, Role.MANAGER )
                put("schedule",  { ctx -> scheduleJob(ctx, server) }, Role.ADMINISTRATOR, Role.MANAGER )
                get("logs",  { ctx -> getJobLogs(ctx) }, Role.ADMINISTRATOR, Role.MANAGER )
                delete("logs",  { ctx -> purgeJobLogs(ctx) }, Role.ADMINISTRATOR, Role.MANAGER )
            }
        }

        /* Endpoints related to participants. */
        get("participants", { ctx -> listParticipants(ctx) }, Role.ADMINISTRATOR, Role.MANAGER )
        path("participants") {
            post("{name}", { ctx -> createParticipants(ctx) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteParticipants(ctx) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to job templates. */
        get("templates", { ctx -> listJobTemplates(ctx) }, Role.ADMINISTRATOR, Role.MANAGER )
        post("templates", { ctx -> createJobTemplate(ctx, server) }, Role.ADMINISTRATOR )
        path("templates") {
            get("{id}",  { ctx -> getJobTemplate(ctx) }, Role.ADMINISTRATOR, Role.MANAGER )
            put("{id}",  { ctx -> updateJobTemplate(ctx, server) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteJobTemplate(ctx, server) }, Role.ADMINISTRATOR )
        }

        /* Endpoint related to Apache Solr configurations. */
        get("solr", { ctx -> listSolrConfigurations(ctx) }, Role.ADMINISTRATOR, Role.MANAGER )
        post("solr", { ctx -> createSolrConfig(ctx) }, Role.ADMINISTRATOR )
        path("solr") {
            get("collections", { ctx -> listSolrCollections(ctx) }, Role.ADMINISTRATOR, Role.MANAGER )
            get("{id}", { ctx -> getSolrConfig(ctx) }, Role.ADMINISTRATOR, Role.MANAGER )
            put("{id}", { ctx -> updateSolrConfig(ctx) }, Role.ADMINISTRATOR )
            delete("{id}", { ctx -> deleteSolrConfig(ctx) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to entity mappings. */
        get("mappings", { ctx -> listEntityMappings(ctx) }, Role.ADMINISTRATOR )
        post("mappings", { ctx -> createEntityMapping(ctx) }, Role.ADMINISTRATOR )
        path("mappings") {
            get("{id}",  { ctx -> getEntityMapping(ctx) }, Role.ADMINISTRATOR )
            put("{id}",  { ctx -> updateEntityMapping(ctx) }, Role.ADMINISTRATOR )
            delete("{id}",  { ctx -> deleteEntityMapping(ctx) }, Role.ADMINISTRATOR )
        }

        /* Endpoints related to OAI-PMH. */
        path("{collection}") {
            val oai = OaiServer()
            get("oai-pmh") { ctx -> getOaiPmh(ctx, oai) }
            post("oai-pmh") { ctx -> postOaiPmh(ctx, oai) }
        }
    }
}
