package ch.pontius.kiar.api.routes.collection

import ch.pontius.kiar.api.model.collection.ObjectCollection
import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.openapi.*
import ch.pontius.kiar.database.collections.Collections
import ch.pontius.kiar.database.collections.Collections.toObjectCollection
import ch.pontius.kiar.database.config.ImageDeployments
import ch.pontius.kiar.database.config.ImageDeployments.toImageDeployment
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.SolrConfigs.toSolr
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.setField
import ch.pontius.kiar.utilities.extensions.queryParam
import com.sksamuel.scrimage.ImmutableImage
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Paths

/** The [KLogger] instance for synchronization endpoint. */
private val logger: KLogger = KotlinLogging.logger {}

val postSyncCollectionsDoc: RouteDoc = {
    operationId = "postSynchronizeCollections"
    summary = "Synchronizes object collections with an Apache Solr backend."
    tags("Collection")
    parameters {
        queryParam("collectionId", "The ID  of the Apache Solr configuration to use.", INT32, required = true)
    }
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun postSyncCollections(call: ApplicationCall) {
    val collectionId = call.queryParam("collectionId")?.toIntOrNull() ?: throw ErrorStatusException(400, "Query parameter 'collectionId' is required.")
    val (config, collectionName, collections) = transaction {
        val (collectionName, config) = (SolrConfigs innerJoin SolrCollections).select(SolrConfigs.columns + SolrCollections.name).where {
            (SolrCollections.id eq collectionId)  and (SolrCollections.type eq CollectionType.COLLECTION)
        }.map {
            it[SolrCollections.name] to it.toSolr()
        }.firstOrNull() ?: throw ErrorStatusException(404, "Apache Solr config with ID $collectionId  could not be found.")

        /* Fetch image deployments. */
        val deployments = ImageDeployments.selectAll().where { ImageDeployments.solrInstanceId eq config.id!! }.map { it.toImageDeployment() }

        /* Fetch collections. */
        val collections = (Collections innerJoin Institutions innerJoin Participants).selectAll().where { Collections.publish eq true }.map { it.toObjectCollection() }

        /* Return triple. */
        Triple(config.copy(deployments = deployments), collectionName, collections)
    }

    /* Perform actual synchronization. */
    synchronise(config, collectionName, collections)

    /* Return success status. */
    call.respond(SuccessStatus("Successfully synchronized object collections."))
}


/**
 * Handles the actual synchronisation logic.
 *
 * @param collection The [ApacheSolrConfig] that specifies the server to synchronise with.
 * @param collection The name of the collection to synchronise with.
 * @param collections The [List] of [ObjectCollection]s to add.
 */
private fun synchronise(config: ApacheSolrConfig, collection: String, collections: List<ObjectCollection>) {
    /* Prepare HTTP client builder. */
    var httpBuilder = HttpJettySolrClient.Builder(config.server)
    if (config.username != null && config.password != null) {
        httpBuilder = httpBuilder.withBasicAuthCredentials(config.username, config.password)
    }
    httpBuilder.build().use { client ->
        try {
            /* Delete all existing entries. */
            var response: UpdateResponse = client.deleteByQuery(collection, "*:*")
            if (response.status == 0) {
                logger.info { "Purged collection (collection = $collection)." }
            } else {
                logger.error {"Failed to purge collection (collection = $collection)." }
            }

            /* Map documents and add them. */
            val documents = collections.map { collection ->
                val doc = SolrInputDocument()
                doc.setField(Field.UUID, collection.uuid ?: throw IllegalArgumentException("Collection UUID is required."))
                doc.setField(Field.PARTICIPANT, collection.institution?.participantName ?: throw IllegalArgumentException("Collection participant is required."))
                doc.setField(Field.CANTON, collection.institution.canton.shortName)
                doc.setField(Field.DISPLAY, collection.displayName)
                doc.setField("name", collection.name)
                doc.setField("institution", collection.institution.name)
                collection.filters.forEach {
                    doc.addField("filters", it)
                }
                doc.setField("description", collection.description)

                /* Add entries for institution image. */
                for (deployment in config.deployments) {
                    for (imageName in collection.images) {
                        val path = Paths.get(deployment.path).resolve("collections").resolve(deployment.name).resolve(imageName)
                        try {
                            val image = ImmutableImage.loader().fromPath(path)
                            if (deployment.server == null) {
                                doc.addField(deployment.name, "/collections/${deployment.name}/$imageName")
                            } else {
                                doc.addField(deployment.name, "${deployment.server}collections/${deployment.name}/$imageName")
                            }
                            doc.addField("${deployment.name}height_", image.height)
                            doc.addField("${deployment.name}width_", image.width)
                        } catch (e: Throwable) {
                            logger.error(e) { "Failed to load image from path: $path" }
                        }
                    }
                }
                doc
            }

            /* Add documents. */
            response = client.add(collection, documents)
            if (response.status == 0) {
                logger.debug { "Ingested ${documents.size} documents (collection = $collection)." }
            } else {
                logger.error { "Failed to ingest documents (collection = $collection)." }
            }

            /* Commit changes. */
            response = client.commit(collection)
            if (response.status == 0) {
                logger.info { "Committed ${documents.size} documents (collection = $collection)." }
            } else {
                logger.error { "Failed to commit documents (collection = $collection)." }
            }
        } catch (e: Throwable) {
            throw ErrorStatusException(500, "Error occurred while trying to purge Apache Solr collection: ${e.message}")
        }
    }
}