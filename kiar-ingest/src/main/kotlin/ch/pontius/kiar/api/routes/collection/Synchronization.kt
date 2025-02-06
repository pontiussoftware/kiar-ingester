package ch.pontius.kiar.api.routes.collection

import ch.pontius.kiar.api.model.collection.ObjectCollection
import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.collection.DbObjectCollection
import ch.pontius.kiar.database.config.solr.DbCollectionType
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.ingester.solrj.Constants
import com.sksamuel.scrimage.ImmutableImage
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Paths

private val LOGGER = LogManager.getLogger()

@OpenApi(
    path = "/api/collections/synchronize",
    methods = [HttpMethod.POST],
    summary = "Synchronizes object collections with an Apache Solr backend.",
    operationId = "postSynchronizeCollections",
    tags = ["Collection"],
    queryParams = [
        OpenApiParam(name = "solr", type = String::class, description = "Name of the Apache Solr configuration to use.", required = true),
        OpenApiParam(name = "collection", type = String::class, description = "The name of the collection to synchronize with.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun postSyncCollections(ctx: Context, store: TransientEntityStore) {
    val configName = ctx.queryParam("solr") ?: throw ErrorStatusException(400, "Query parameter 'solr' is required.")
    val collectionName = ctx.queryParam("collection") ?: throw ErrorStatusException(400, "Query parameter 'collectionName' is required.")
    store.transactional(true) {
        val collection = DbSolr.filter {
            it.name eq configName
        }.flatMapDistinct {
            it.collections
        }.filter {
            (it.name eq collectionName) and (it.type eq DbCollectionType.COLLECTION)
        }.firstOrNull() ?: throw ErrorStatusException(404, "Apache Solr collection with name $collectionName could not be found.")

        val config = collection.solr.toApi()
        val collections = DbObjectCollection.filter { it.publish eq true }.toList()
        synchronise(config, collectionName, collections)
    }

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
private fun synchronise(config: ApacheSolrConfig, collection: String, collections: List<DbObjectCollection>) {
    /* Prepare HTTP client builder. */
    var httpBuilder = Http2SolrClient.Builder(config.server)
    if (config.username != null && config.password != null) {
        httpBuilder = httpBuilder.withBasicAuthCredentials(config.username, config.password)
    }
    httpBuilder.build().use { client ->
        try {
            /* Delete all existing entries. */
            var response = client.deleteByQuery(collection, "*:*")
            if (response.status == 0) {
                LOGGER.info("Purged collection (collection = {}).", collection)
            } else {
                LOGGER.error("Failed to purge collection (collection = {}).", collection)
            }

            /* Map documents and add them. */
            val documents = collections.map { collection ->
                val doc = SolrInputDocument()
                doc.setField(Constants.FIELD_NAME_UUID, collection.xdId)
                doc.setField(Constants.FIELD_NAME_PARTICIPANT, collection.institution.participant.name)
                doc.setField(Constants.FIELD_NAME_CANTON, collection.institution.canton)
                doc.setField(Constants.FIELD_NAME_DISPLAY, collection.displayName)
                doc.setField("name", collection.name)
                doc.setField("institution", collection.institution.name)
                collection.filters.forEach {
                    doc.addField("filters", it)
                }
                doc.setField("description", collection.description)

                /* Add entries for institution image. */
                for (deployment in config.deployments) {
                    for (imageName in collection.images) {
                        val path = Paths.get(deployment.path).resolve("collections").resolve(imageName)
                        try {
                            val image = ImmutableImage.loader().fromPath(path)
                            if (deployment.server == null) {
                                doc.addField(deployment.name, "/collections/${deployment.name}/$image")
                            } else {
                                doc.addField(deployment.name, "${deployment.server}collections/${deployment.name}/$image")
                            }
                            doc.addField("${deployment.name}height_", image.height)
                            doc.addField("${deployment.name}width_", image.width)
                        } catch (e: Throwable) {
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