package ch.pontius.kiar.api.routes.collection

import ch.pontius.kiar.api.model.collection.ObjectCollection
import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.collections.Collections
import ch.pontius.kiar.database.collections.Collections.toObjectCollection
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.SolrConfigs.toSolr
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.setField
import com.sksamuel.scrimage.ImmutableImage
import io.javalin.http.Context
import io.javalin.openapi.*
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Paths

private val LOGGER = LogManager.getLogger()

@OpenApi(
    path = "/api/collections/synchronize",
    methods = [HttpMethod.POST],
    summary = "Synchronizes object collections with an Apache Solr backend.",
    operationId = "postSynchronizeCollections",
    tags = ["Collection"],
    queryParams = [
        OpenApiParam(name = "collectionId", type = Int::class, description = "The ID  of the Apache Solr configuration to use.", required = true),
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun postSyncCollections(ctx: Context) {
    val collectionId = ctx.queryParam("collectionId")?.toIntOrNull() ?: throw ErrorStatusException(400, "Query parameter 'collectionId' is required.")
    val (config, collectionName, collections) = transaction {
        val (collectionName, config) = (SolrConfigs innerJoin SolrCollections).select(SolrConfigs.columns + SolrCollections.name).where {
            (SolrCollections.id eq collectionId)  and (SolrCollections.type eq CollectionType.COLLECTION)
        }.map {
            it[SolrCollections.name] to it.toSolr()
        }.firstOrNull() ?: throw ErrorStatusException(404, "Apache Solr config with ID $collectionId  could not be found.")

        val collections = (Collections innerJoin Institutions innerJoin Participants).selectAll().where { Collections.publish eq true }.map { it.toObjectCollection() }
        Triple(config, collectionName, collections)
    }

    /* Perform actual synchronization. */
    synchronise(config, collectionName, collections)

    /* Return success status. */
    ctx.json(SuccessStatus("Successfully synchronized object collections."))
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
    var httpBuilder = Http2SolrClient.Builder(config.server)
    if (config.username != null && config.password != null) {
        httpBuilder = httpBuilder.withBasicAuthCredentials(config.username, config.password)
    }
    httpBuilder.build().use { client ->
        try {
            /* Delete all existing entries. */
            var response: UpdateResponse = client.deleteByQuery(collection, "*:*")
            if (response.status == 0) {
                LOGGER.info("Purged collection (collection = {}).", collection)
            } else {
                LOGGER.error("Failed to purge collection (collection = {}).", collection)
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
                        } catch (_: Throwable) {
                            LOGGER.error("Failed to load image from path: $path")
                        }
                    }
                }
                doc
            }

            /* Add documents. */
            response = client.add(collection, documents)
            if (response.status == 0) {
                LOGGER.debug("Ingested {} documents (collection = {}).", documents.size, collection)
            } else {
                LOGGER.error("Failed to ingest documents (collection = {}).", collection)
            }

            /* Commit changes. */
            response = client.commit(collection)
            if (response.status == 0) {
                LOGGER.info("Committed {} documents (collection = {}).", documents.size, collection)
            } else {
                LOGGER.error("Failed to commit documents (collection = {}).", collection)
            }
        } catch (e: Throwable) {
            throw ErrorStatusException(500, "Error occurred while trying to purge Apache Solr collection: ${e.message}")
        }
    }
}