package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.config.image.ImageDeployment
import ch.pontius.kiar.api.model.config.solr.ApacheSolrCollection
import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.SolrConfigId
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.openapi.*
import ch.pontius.kiar.database.config.ImageDeployments
import ch.pontius.kiar.database.config.ImageDeployments.toImageDeployment
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrCollections.toSolrCollection
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.SolrConfigs.toSolr
import ch.pontius.kiar.utilities.extensions.pathParam
import ch.pontius.kiar.utilities.extensions.receiveOrThrow
import ch.pontius.kiar.utilities.extensions.withSuffix
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

val listSolrConfigurationsDoc: RouteDoc = {
    operationId = "getListSolrConfiguration"
    summary = "Lists all available Apache Solr configurations."
    tags("Config", "Apache Solr")
    responses {
        json<List<ApacheSolrConfig>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listSolrConfigurations(call: ApplicationCall) {
    val results = transaction {
        SolrConfigs.selectAll().orderBy(SolrConfigs.name to SortOrder.ASC).map { it.toSolr() }
    }
    call.respond(results)
}

val listSolrCollectionsDoc: RouteDoc = {
    operationId = "getListSolrCollections"
    summary = "Lists all available Apache Solr collections."
    tags("Config", "Apache Solr")
    responses {
        json<List<ApacheSolrCollection>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listSolrCollections(call: ApplicationCall) {
    val results = transaction {
        SolrCollections.selectAll().orderBy(SolrCollections.name to SortOrder.ASC).map { it.toSolrCollection() }
    }
    call.respond(results)
}

val getSolrConfigDoc: RouteDoc = {
    operationId = "getSolrConfig"
    summary = "Retrieves all the details about an Apache Solr configuration."
    tags("Config", "Apache Solr")
    parameters {
        pathParam("id", "The ID of the Apache Solr configuration that should be deleted.")
    }
    responses {
        json<ApacheSolrConfig>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun getSolrConfig(call: ApplicationCall) {
    val solrId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed Apache Solr Configuration ID.")
    val config = transaction {
        val config = SolrConfigs.selectAll().where { SolrConfigs.id eq solrId }.map { it.toSolr() }.firstOrNull() ?: throw ErrorStatusException(404, "Could not find Apache Solr Configuration with ID $solrId.")
        val collections = SolrCollections.selectAll().where { SolrCollections.solrInstanceId eq config.id }.map { it.toSolrCollection() }
        val deployments = ImageDeployments.selectAll().where { ImageDeployments.solrInstanceId eq config.id }.map { it.toImageDeployment() }
        config.copy(collections = collections,  deployments = deployments)
    }
    call.respond(config)
}

val createSolrConfigDoc: RouteDoc = {
    operationId = "postCreateSolrConfig"
    summary = "Creates a new Apache Solr configuration."
    tags("Config", "Apache Solr")
    jsonBody<ApacheSolrConfig>()
    responses {
        json<ApacheSolrConfig>(200)
        errors(400, 401, 403, 500)
    }
}

suspend fun createSolrConfig(call: ApplicationCall) {
    val request = call.receiveOrThrow<ApacheSolrConfig>()
    val created = transaction {
        val solrConfigId = SolrConfigs.insertAndGetId { config ->
            config[name] = request.name
            config[server] = request.server
            config[publicServer] = request.publicServer
            config[username] = request.username
            config[password] = request.password?.ifEmpty { null }
        }.value

        /* Create collection entries. */
        mergeCollections(solrConfigId, request.collections)

        /* Create deployment entries. */
        mergeDeployments(solrConfigId, request.deployments)

        /* Return copy with ID (and without the password). */
        request.copy(id = solrConfigId, password = null)
    }
    call.respond(created)
}

val updateSolrConfigDoc: RouteDoc = {
    operationId = "updateSolrConfig"
    summary = "Updates an existing Apache Solr configuration."
    tags("Config", "Apache Solr")
    parameters {
        pathParam("id", "The ID of the Apache Solr configuration that should be updated.")
    }
    jsonBody<ApacheSolrConfig>()
    responses {
        json<ApacheSolrConfig>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun updateSolrConfig(call: ApplicationCall) {
    /* Extract the ID and the request body. */
    val solrId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed Apache Solr Configuration ID.")
    val request = call.receiveOrThrow<ApacheSolrConfig>()

    /* Start transaction and apply changes. */
    transaction {
        SolrConfigs.update({ SolrConfigs.id eq solrId }) { config ->
            config[name] = request.name
            config[description] = request.description
            config[server] = request.server.withSuffix("/")
            config[publicServer] = request.publicServer
            config[username] = request.username
            if (!request.password.isNullOrEmpty()) {
                /* The API never returns the password, so an empty value means "keep the existing one". */
                config[password] = request.password
            }
            config[modified] = Instant.now()
        }

        /* Create collection entries. */
        mergeCollections(solrId, request.collections)

        /* Create deployment entries. */
        mergeDeployments(solrId, request.deployments)
    }

    call.respond(request.copy(password = null))
}

val deleteSolrConfigDoc: RouteDoc = {
    operationId = "deleteSolrConfig"
    summary = "Deletes an existing Apache Solr configuration."
    tags("Config", "Apache Solr")
    parameters {
        pathParam("id", "The ID of the Apache Solr configuration that should be deleted.")
    }
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun deleteSolrConfig(call: ApplicationCall) {
    val solrId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed configuration ID")
    val deleted = transaction {
        SolrCollections.deleteWhere { SolrCollections.id eq solrId }
    }
    if (deleted > 0) {
        call.respond(SuccessStatus("Apache Solr configuration $solrId deleted successfully."))
    } else {
        call.respond(ErrorStatus(404, "Apache Solr configuration with ID $solrId could not be found."))
    }
}

/**
 * Overrides all [ApacheSolrCollection]s for the given [SolrConfigId]. Used during insert and update.
 *
 * @param solrConfigId [SolrConfigId] of the [ApacheSolrConfig]
 * @param collections [List] of [ApacheSolrCollection]s to merge.
 */
private fun mergeCollections(solrConfigId: SolrConfigId, collections: List<ApacheSolrCollection>) {
    /* Delete all existing entries. */
    SolrCollections.deleteWhere { (SolrCollections.solrInstanceId eq solrConfigId) and (SolrCollections.id notInList (collections.mapNotNull { it.id }))}

    /* Insert or update entries. */
    for (c in collections) {
        if (c.id != null) {
            SolrCollections.update({ SolrCollections.id eq c.id}) { update ->
                update[SolrCollections.name] = c.name
                update[SolrCollections.displayName] = c.displayName
                update[SolrCollections.type] = c.type
                update[SolrCollections.selector] = c.selector
                update[SolrCollections.deleteBeforeIngest] = c.deleteBeforeIngest
                update[SolrCollections.oai] = c.oai
                update[SolrCollections.sru] = c.sru
            }
        } else {
            SolrCollections.insert { insert ->
                insert[SolrCollections.solrInstanceId] = solrConfigId
                insert[SolrCollections.name] = c.name
                insert[SolrCollections.displayName] = c.displayName
                insert[SolrCollections.type] = c.type
                insert[SolrCollections.selector] = c.selector
                insert[SolrCollections.deleteBeforeIngest] = c.deleteBeforeIngest
                insert[SolrCollections.oai] = c.oai
                insert[SolrCollections.sru] = c.sru
            }
        }
    }
}

/**
 * Overrides all [ImageDeployment]s for the given [SolrConfigId]. Used during insert and update.
 *
 * @param solrConfigId [SolrConfigId] of the [ApacheSolrConfig]
 * @param deployments [List] of [ImageDeployment]s to merge.
 */
private fun mergeDeployments(solrConfigId: SolrConfigId, deployments: List<ImageDeployment>) {
    /* Delete all existing entries. */
    ImageDeployments.deleteWhere { ImageDeployments.solrInstanceId eq solrConfigId }

    /* Re-create entries. */
    for (d in deployments) {
        ImageDeployments.insert { deployment ->
            if (d.id != null) deployment[ImageDeployments.id] = d.id
            deployment[ImageDeployments.solrInstanceId] = solrConfigId
            deployment[ImageDeployments.name] = d.name
            deployment[ImageDeployments.format] = d.format
            deployment[ImageDeployments.src] = d.source
            deployment[ImageDeployments.server] = d.server?.withSuffix("/")
            deployment[ImageDeployments.path] = d.path
            deployment[ImageDeployments.maxSize] = d.maxSize
        }
    }
}