package ch.pontius.kiar.api.routes.institution

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.SolrConfigs.toSolr
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Institutions.toInstitution
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.setField
import io.javalin.http.Context
import io.javalin.openapi.*
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.SolrInputDocument
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val LOGGER = LogManager.getLogger()

@OpenApi(
    path = "/api/institutions/synchronize",
    methods = [HttpMethod.POST],
    summary = "Synchronizes institutions with an Apache Solr backend.",
    operationId = "postSynchronizeInstitutions",
    tags = ["Institution"],
    queryParams = [
        OpenApiParam(name = "collectionId", type = Int::class, description = "The ID of the Apache Solr collection to synchronize with.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun postSyncInstitutions(ctx: Context) {
    val collectionId = ctx.queryParam("collectionId")?.toIntOrNull() ?: throw ErrorStatusException(400, "Query parameter 'collectionId' is required.")
    val (config, collectionName, institutions) = transaction {
        val (collectionName, config) = (SolrConfigs innerJoin SolrCollections).select(SolrConfigs.columns + SolrCollections.name).where {
            (SolrCollections.id eq collectionId) and (SolrCollections.type eq CollectionType.MUSEUM)
        }.map {
            it[SolrCollections.name] to it.toSolr()
        }.firstOrNull() ?: throw ErrorStatusException(404, "Apache Solr config for collection with ID $collectionId could not be found.")

        val institutions = (Institutions innerJoin Participants).selectAll().where { Institutions.publish eq true }.map { it.toInstitution() }
        Triple(config,collectionName, institutions)
    }

    /* Perform actual synchronisation. */
    synchronise(config, collectionName, institutions)

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
    httpBuilder.build().use { client ->
        try {
            /* Delete all existing entries. */
            var response = client.deleteByQuery(collection, "*:*")
            if (response?.status == 0) {
                LOGGER.info("Purged collection (collection = {}).", collection)
            } else {
                LOGGER.error("Failed to purge collection (collection = {}).", collection)
            }

            /* Map documents and add them. */
            val documents = institutions.map { institution ->
                val doc = SolrInputDocument()
                doc.setField(Field.UUID, institution.uuid ?: throw IllegalArgumentException("Institution UUID is required."))
                doc.setField(Field.PARTICIPANT, institution.participantName)
                doc.setField(Field.CANTON, institution.canton)
                doc.setField(Field.DISPLAY, institution.displayName)
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