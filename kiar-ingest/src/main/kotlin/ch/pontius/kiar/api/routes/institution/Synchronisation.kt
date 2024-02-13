package ch.pontius.kiar.api.routes.institution

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.config.solr.DbCollectionType
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.ingester.solrj.Constants
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.flatMapDistinct
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.SolrInputDocument

@OpenApi(
    path = "/api/institutions/synchronize",
    methods = [HttpMethod.POST],
    summary = "Synchronizes institutions with an Apache Solr backend.",
    operationId = "postSynchronizeInstitutions",
    tags = ["Institution"],
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
fun postSyncInstitutions(ctx: Context, store: TransientEntityStore) {
    val configName = ctx.queryParam("solr") ?: throw ErrorStatusException(400, "Query parameter 'solr' is required.")
    val collectionName = ctx.queryParam("collection") ?: throw ErrorStatusException(400, "Query parameter 'collectionName' is required.")
    val data = store.transactional(true) {
        val collection = DbSolr.filter {
            it.name eq configName
        }.flatMapDistinct {
            it.collections
        }.filter {
            (it.name eq collectionName) and (it.type eq DbCollectionType.MUSEUM)
        }.firstOrNull() ?: throw ErrorStatusException(404, "Apache Solr collection with name $collectionName could not be found.")

        val config = collection.solr.toApi()
        val institutions = DbInstitution.filter { it.publish eq true }.asSequence().map { it.toApi() }.toList()
        config to institutions
    }

    /* Perform actual synchronisation. */
    synchronise(data.first, collectionName, data.second)

    /* Return success status. */
    ctx.json(SuccessStatus("Successfully synchronized institutions."))
}


/**
 * Handles the actual synchronisation logic.
 *
 * @param collection The [ApacheSolrConfig] that specifies the server to synchronise with.
 * @param collection The name of the collection to synchronise with.
 * @param institutions The [List] of [Institution] to add.
 */
private fun synchronise(config: ApacheSolrConfig, collection: String, institutions: List<Institution>) {
    /* Prepare HTTP client builder. */
    var httpBuilder = Http2SolrClient.Builder(config.server)
    if (config.username != null && config.password != null) {
        httpBuilder = httpBuilder.withBasicAuthCredentials(config.username, config.password)
    }
    val client = httpBuilder.build()
    client.use {
        try {
            /* Delete all existing entries. */
            client.deleteByQuery(collection, "*:*")

            /* Map documents and add them. */
            val documents = institutions.map { institution ->
                val doc = SolrInputDocument()
                doc.setField(Constants.FIELD_NAME_UUID, institution.id)
                doc.setField(Constants.FIELD_NAME_PARTICIPANT, institution.participantName)
                doc.setField(Constants.FIELD_NAME_CANTON, institution.canton)
                doc.setField(Constants.FIELD_NAME_DISPLAY, institution.displayName)
                if (institution.isil != null) doc.setField("isil", institution.isil)
                doc.setField("name", institution.name)
                doc.setField("name_s", institution.name)
                doc.setField("description", institution.description)
                doc.setField("street", institution.street)
                doc.setField("city", institution.city)
                doc.setField("zip", institution.zip)
                doc.setField("email", institution.email)
                doc.setField("website", institution.homepage)

                /* Add entries for institution image. */
                if (institution.imageName != null) {
                    for (deployment in config.deployments) {
                        if (deployment.server == null) {
                            doc.setField(deployment.name, "/institutions/${deployment.name}/${institution.imageName}")
                        } else {
                            doc.setField(deployment.name, "${deployment.server}institutions/${deployment.name}/${institution.imageName}")
                        }
                    }
                }


                if (institution.longitude != null && institution.latitude != null) {
                    doc.setField("koordinaten_wgs84", "${institution.latitude},${institution.longitude}")
                }

                doc
            }

            client.add(collection, documents)
            client.commit(collection)
        } catch (e: Throwable) {
            throw ErrorStatusException(500, "Error occurred while trying to purge Apache Solr collection: ${e.message}")
        }
    }
}