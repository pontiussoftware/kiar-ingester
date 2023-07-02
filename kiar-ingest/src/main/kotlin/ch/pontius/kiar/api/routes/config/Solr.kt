package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.config.solr.SolrConfig
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.config.CollectionConfig
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.util.findById

@OpenApi(
    path = "/api/solr",
    methods = [HttpMethod.GET],
    summary = "Lists all available Apache Solr configurations.",
    operationId = "getListSolrConfiguration",
    tags = ["Config", "Apache Solr"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<SolrConfig>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listSolrConfigurations(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        ctx.json(DbSolr.all().mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/solr",
    methods = [HttpMethod.POST],
    summary = "Creates a new Apache Solr configuration.",
    operationId = "postCreateSolrConfig",
    tags = ["Config", "Apache Solr"],
    pathParams = [],
    requestBody = OpenApiRequestBody([OpenApiContent(SolrConfig::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(SolrConfig::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun createSolrConfig(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(SolrConfig::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }
    val created = store.transactional {
        /* Update basic properties. */
        val solr = DbSolr.new {
            name = request.name
            server = request.server
            username = request.user
            password = request.password
        }

        /* Now merge collection data. */
        solr.merge(request.collections)
        solr.toApi()
    }
    ctx.json(created)
}

@OpenApi(
    path = "/api/solr/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing Apache Solr configuration.",
    operationId = "deleteSolrConfig",
    tags = ["Config", "Apache Solr"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the Apache Solr configuration that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteSolrConfig(ctx: Context, store: TransientEntityStore) {
    val solrId = ctx.pathParam("id")
    store.transactional {
        val mapping = try {
            DbSolr.findById(solrId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Apache Solr configuration with ID $solrId could not be found.")
        }
        mapping.delete()
    }
    ctx.json(SuccessStatus("Apache Solr configuration $solrId deleted successfully."))
}

@OpenApi(
    path = "/api/solr/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing Apache Solr configuration.",
    operationId = "updateSolrConfig",
    tags = ["Config", "Apache Solr"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the Apache Solr configuration that should be updated.", required = true)
    ],
    requestBody = OpenApiRequestBody([OpenApiContent(SolrConfig::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(SolrConfig::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun updateSolrConfig(ctx: Context, store: TransientEntityStore) {
    /* Extract the ID and the request body. */
    val solrId = ctx.pathParam("id")
    val request = try {
        ctx.bodyAsClass(SolrConfig::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }

    /* Start transaction and apply changes. */
    val updated = store.transactional {
        val solr = try {
            DbSolr.findById(solrId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Apache Solr configuration with ID $solrId could not be found.")
        }

        /* Update basic properties. */
        solr.name = request.name
        solr.server = request.server
        solr.username = request.user
        solr.password = request.password

        /* Now merge attribute mappings. */
        solr.merge(request.collections)
        solr.toApi()
    }

    ctx.json(updated)
}

/**
 * Overrides a [DbSolr]'s [DbCollection]s using the provided list.
 *
 * @param collections [List] of [CollectionConfig]s to merge [DbSolr] with.
 */
private fun DbSolr.merge(collections: List<CollectionConfig>) {
    this.collections.clear()
    for (c in collections) {
        this.collections.add(DbCollection.new {
            name = c.name
            filters = c.filter.joinToString(DbCollection.FILTER_ENTRY_DELIMITER)
            acceptEmptyFilter = c.acceptEmptyFilter
            deleteBeforeIngest = c.deleteBeforeImport
        })
    }
}