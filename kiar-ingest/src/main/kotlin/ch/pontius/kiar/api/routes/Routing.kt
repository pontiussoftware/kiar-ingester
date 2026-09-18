package ch.pontius.kiar.api.routes

import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.routes.collection.*
import ch.pontius.kiar.api.routes.config.*
import ch.pontius.kiar.api.routes.institution.*
import ch.pontius.kiar.api.routes.job.*
import ch.pontius.kiar.api.routes.masterdata.*
import ch.pontius.kiar.api.routes.publication.*
import ch.pontius.kiar.api.routes.session.*
import ch.pontius.kiar.api.routes.user.*
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.servers.oai.OaiServer
import ch.pontius.kiar.servers.sru.SruServer
import createEntityMapping
import createEntityMappingDoc
import deleteEntityMapping
import deleteEntityMappingDoc
import getEntityMapping
import getEntityMappingDoc
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import listEntityMappings
import listEntityMappingsDoc
import updateEntityMapping
import updateEntityMappingDoc

/**
 * Runs a request handler on [Dispatchers.IO], since handlers perform blocking database and file system work.
 *
 * @param block The handler to execute.
 */
private suspend inline fun io(crossinline block: suspend () -> Unit) = withContext(Dispatchers.IO) { block() }

/**
 * Configures all the API routes.
 *
 * @param config The program [Config].
 * @param server The [IngesterServer] instance.
 * @param oaiServer The [OaiServer] instance.
 * @param sruServer The [SruServer] instance.
 * @return The API root [Route].
 */
@OptIn(ExperimentalKtorApi::class)
fun Route.configureApiRoutes(config: Config, server: IngesterServer, oaiServer: OaiServer, sruServer: SruServer): Route = route("api") {
    /* All paths related to session, login and logout handling. */
    route("session") {
        post("login") { io { login(call) } }.describe(loginDoc)
        authorized(Role.ADMINISTRATOR, Role.VIEWER, Role.MANAGER) {
            get("logout") { io { logout(call) } }.describe(logoutDoc)
            get("status") { io { status(call) } }.describe(statusDoc)
            get("user") { io { getUser(call) } }.describe(getUserDoc)
            put("user") { io { updateUser(call) } }.describe(updateUserDoc)
        }
    }

    /* Endpoints related to user management. */
    authorized(Role.ADMINISTRATOR) {
        get("users") { io { getListUsers(call) } }.describe(getListUsersDoc)
        post("users") { io { postCreateUser(call) } }.describe(postCreateUserDoc)
        route("users") {
            get("roles") { io { getListRoles(call) } }.describe(getListRolesDoc)
            put("{id}") { io { putUpdateUser(call) } }.describe(putUpdateUserDoc)
            delete("{id}") { io { deleteUser(call) } }.describe(deleteUserDoc)
        }
    }

    /* Endpoints related to institutions. */
    authorized(Role.ADMINISTRATOR) {
        get("institutions") { io { getListInstitutions(call) } }.describe(getListInstitutionsDoc)
        post("institutions") { io { postCreateInstitution(call) } }.describe(postCreateInstitutionDoc)
        route("institutions") {
            get("name") { io { getListInstitutionNames(call) } }.describe(getListInstitutionNamesDoc)
            post("synchronize") { io { postSyncInstitutions(call) } }.describe(postSyncInstitutionsDoc)
            delete("{id}") { io { deleteInstitution(call) } }.describe(deleteInstitutionDoc)
        }
    }
    authorized(Role.ADMINISTRATOR, Role.MANAGER) {
        route("institutions/{id}") {
            get { io { getInstitution(call) } }.describe(getInstitutionDoc)
            put { io { putUpdateInstitution(call) } }.describe(putUpdateInstitutionDoc)
            post("image") { io { postUploadImageForInstitution(call) } }.describe(postUploadImageForInstitutionDoc)
        }
    }
    authorized(Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER) {
        get("institutions/{id}/image") { io { getImageForInstitution(call) } }.describe(getImageForInstitutionDoc)
    }

    /* Endpoints related to collections. */
    authorized(Role.ADMINISTRATOR) {
        get("collections") { io { getListCollections(call) } }.describe(getListCollectionsDoc)
        post("collections") { io { postCreateCollection(call) } }.describe(postCreateCollectionDoc)
        route("collections") {
            post("synchronize") { io { postSyncCollections(call) } }.describe(postSyncCollectionsDoc)
            delete("{id}") { io { deleteCollection(call) } }.describe(deleteCollectionDoc)
        }
    }
    authorized(Role.ADMINISTRATOR, Role.MANAGER) {
        route("collections/{id}") {
            get { io { getCollection(call) } }.describe(getCollectionDoc)
            put { io { putUpdateCollection(call) } }.describe(putUpdateCollectionDoc)
            post { io { postUploadImageForCollection(call) } }.describe(postUploadImageForCollectionDoc)
        }
    }
    authorized(Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER) {
        route("collections/{id}/{name}") {
            get { io { getImageForCollection(call) } }.describe(getImageForCollectionDoc)
            delete { io { deleteImageForCollection(call) } }.describe(deleteImageForCollectionDoc)
        }
    }

    /* Endpoints related to master data. */
    authorized(Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER) {
        route("masterdata") {
            get("rightstatements") { io { listRightStatements(call) } }.describe(listRightStatementsDoc)
            get("cantons") { io { listCantons(call) } }.describe(listCantonsDoc)
            get("transformers") { io { listTransformerTypes(call) } }.describe(listTransformerTypesDoc)
            get("parsers") { io { listParsers(call) } }.describe(listParsersDoc)
            get("image-formats") { io { listImageFormats(call) } }.describe(listImageFormatsDoc)
            get("mapping-formats") { io { listMappingFormats(call) } }.describe(listMappingFormatsDoc)
            get("job-types") { io { listJobTemplateTypes(call) } }.describe(listJobTemplateTypesDoc)
        }
    }

    /* Endpoints related to jobs. */
    authorized(Role.ADMINISTRATOR, Role.MANAGER) {
        post("jobs") { io { createJob(call) } }.describe(createJobDoc)
        route("jobs") {
            delete("{id}") { io { abortJob(call, server) } }.describe(abortJobDoc)
            route("{id}") {
                put("upload") { io { upload(call, config) } }.describe(uploadDoc)
                put("schedule") { io { scheduleJob(call, server) } }.describe(scheduleJobDoc)
                get("logs") { io { getJobLogs(call) } }.describe(getJobLogsDoc)
                delete("logs") { io { purgeJobLogs(call) } }.describe(purgeJobLogsDoc)
            }
        }
    }
    authorized(Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER) {
        route("jobs") {
            get("active") { io { getActiveJobs(call, server) } }.describe(getActiveJobsDoc)
            get("inactive") { io { getInactiveJobs(call) } }.describe(getInactiveJobsDoc)
        }
    }

    /* Endpoints related to participants. */
    authorized(Role.ADMINISTRATOR, Role.MANAGER) {
        get("participants") { io { listParticipants(call) } }.describe(listParticipantsDoc)
    }
    authorized(Role.ADMINISTRATOR) {
        route("participants") {
            post("{name}") { io { createParticipants(call) } }.describe(createParticipantsDoc)
            delete("{id}") { io { deleteParticipants(call) } }.describe(deleteParticipantsDoc)
        }
    }

    /* Endpoints related to job templates. */
    authorized(Role.ADMINISTRATOR, Role.MANAGER) {
        get("templates") { io { listJobTemplates(call) } }.describe(listJobTemplatesDoc)
        get("templates/{id}") { io { getJobTemplate(call) } }.describe(getJobTemplateDoc)
    }
    authorized(Role.ADMINISTRATOR) {
        post("templates") { io { createJobTemplate(call, server) } }.describe(createJobTemplateDoc)
        route("templates/{id}") {
            put { io { updateJobTemplate(call, server) } }.describe(updateJobTemplateDoc)
            delete { io { deleteJobTemplate(call, server) } }.describe(deleteJobTemplateDoc)
        }
    }

    /* Endpoint related to Apache Solr configurations. */
    authorized(Role.ADMINISTRATOR, Role.MANAGER) {
        get("solr") { io { listSolrConfigurations(call) } }.describe(listSolrConfigurationsDoc)
        route("solr") {
            get("collections") { io { listSolrCollections(call) } }.describe(listSolrCollectionsDoc)
            get("{id}") { io { getSolrConfig(call) } }.describe(getSolrConfigDoc)
        }
    }
    authorized(Role.ADMINISTRATOR) {
        post("solr") { io { createSolrConfig(call) } }.describe(createSolrConfigDoc)
        route("solr/{id}") {
            put { io { updateSolrConfig(call) } }.describe(updateSolrConfigDoc)
            delete { io { deleteSolrConfig(call) } }.describe(deleteSolrConfigDoc)
        }
    }

    /* Endpoints related to entity mappings. */
    authorized(Role.ADMINISTRATOR) {
        get("mappings") { io { listEntityMappings(call) } }.describe(listEntityMappingsDoc)
        post("mappings") { io { createEntityMapping(call) } }.describe(createEntityMappingDoc)
        route("mappings/{id}") {
            get { io { getEntityMapping(call) } }.describe(getEntityMappingDoc)
            put { io { updateEntityMapping(call) } }.describe(updateEntityMappingDoc)
            delete { io { deleteEntityMapping(call) } }.describe(deleteEntityMappingDoc)
        }
    }

    /* Endpoints related to OAI-PMH and SRU (public). */
    route("{collection}") {
        get("oai-pmh") { io { getOaiPmh(call, oaiServer) } }.describe(getOaiPmhDoc)
        post("oai-pmh") { io { postOaiPmh(call, oaiServer) } }.describe(postOaiPmhDoc)
        get("sru") { io { getSruSearch(call, sruServer) } }.describe(getSruSearchDoc)
    }
}
