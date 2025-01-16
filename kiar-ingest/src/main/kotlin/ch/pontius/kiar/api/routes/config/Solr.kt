package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.config.image.ImageDeployment
import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.config.solr.ApacheSolrCollection
import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.config.CollectionConfig
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbImageDeployment
import ch.pontius.kiar.database.config.solr.DbImageFormat
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.database.config.transformers.DbTransformer
import ch.pontius.kiar.utilities.mapToArray
import ch.pontius.kiar.utilities.withSuffix
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.sortedBy
import kotlinx.dnq.util.findById
import org.joda.time.DateTime

@OpenApi(
    path = "/api/solr",
    methods = [HttpMethod.GET],
    summary = "Lists all available Apache Solr configurations.",
    operationId = "getListSolrConfiguration",
    tags = ["Config", "Apache Solr"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<ApacheSolrConfig>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listSolrConfigurations(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        ctx.json(DbSolr.all().sortedBy(DbSolr::name).mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/solr/formats",
    methods = [HttpMethod.GET],
    summary = "Lists all available formats available for image deployment.",
    operationId = "getListImageFormats",
    tags = ["Config",  "Apache Solr"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<ImageFormat>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listFormats(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        val parsers = DbImageFormat.all()
        ctx.json(parsers.mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/solr",
    methods = [HttpMethod.POST],
    summary = "Creates a new Apache Solr configuration.",
    operationId = "postCreateSolrConfig",
    tags = ["Config", "Apache Solr"],
    pathParams = [],
    requestBody = OpenApiRequestBody([OpenApiContent(ApacheSolrConfig::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(ApacheSolrConfig::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun createSolrConfig(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(ApacheSolrConfig::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }
    val created = store.transactional {
        /* Update basic properties. */
        val solr = DbSolr.new {
            name = request.name
            server = request.server
            publicServer = request.publicServer
            username = request.username
            password = request.password
            createdAt = DateTime.now()
            changedAt = DateTime.now()
        }

        /* Now merge collection data. */
        solr.mergeCollections(request.collections)
        solr.mergeDeployments(request.deployments)
        solr.toApi()
    }
    ctx.json(created)
}

@OpenApi(
    path = "/api/solr/{id}",
    methods = [HttpMethod.GET],
    summary = "Retrieves all the details about an Apache Solr configuration.",
    operationId = "getSolrConfig",
    tags = ["Config", "Apache Solr"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the Apache Solr configuration that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(ApacheSolrConfig::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getSolrConfig(ctx: Context, store: TransientEntityStore) {
    val solrId = ctx.pathParam("id")
    val mapping = store.transactional(true) {
        try {
            DbSolr.findById(solrId).toApi()
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Apache Solr configuration with ID $solrId could not be found.")
        }
    }
    ctx.json(mapping)
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
    requestBody = OpenApiRequestBody([OpenApiContent(ApacheSolrConfig::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(ApacheSolrConfig::class)]),
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
        ctx.bodyAsClass(ApacheSolrConfig::class.java)
    } catch (e: Throwable) {
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
        solr.description = request.description
        solr.server = request.server.withSuffix("/")
        solr.publicServer = request.publicServer?.withSuffix("/")
        solr.username = request.username
        solr.password = request.password
        solr.changedAt = DateTime.now()

        /* Now merge attribute mappings. */
        solr.mergeCollections(request.collections)
        solr.mergeDeployments(request.deployments)
        solr.toApi()
    }

    ctx.json(updated)
}

/**
 * Overrides a [DbSolr]'s [DbCollection]s using the provided list.
 *
 * @param collections [List] of [CollectionConfig]s to merge [DbSolr] with.
 */
private fun DbSolr.mergeCollections(collections: List<ApacheSolrCollection>) {
    /* Update and/or add collections. */
    for (c in collections) {
        var collection = this.collections.filter { it.name eq c.name }.firstOrNull()
        if (collection == null) {
            collection = DbCollection.new {  }
            this.collections.add(collection)
        }

        /* Update attributes. */
        collection.name = c.name
        collection.displayName = c.displayName
        collection.type = c.type.toDb()
        collection.selector = c.selector
        collection.deleteBeforeIngest = c.deleteBeforeIngest
        collection.oai = c.oai
    }

    /* Delete collections that have been removed. */
    for (c in this.collections.asSequence()) {
        if (collections.none { it.name == c.name }) {
            c.delete()
        }
    }
}

/**
 * Overrides a [DbJobTemplate]'s [DbTransformer]s using the provided list.
 *
 * @param deployments [List] of [ImageDeployment]s to merge [DbJobTemplate] with.
 */
private fun DbSolr.mergeDeployments(deployments: List<ImageDeployment>) {
    for (d in deployments) {
        var deployment = this.deployments.filter { it.name eq d.name }.firstOrNull()
        if (deployment == null) {
            deployment = DbImageDeployment.new {  }
            this.deployments.add(deployment)
        }

        /* Update attributes. */
        deployment.name = d.name
        deployment.format = d.format.toDb()
        deployment.source = d.source
        deployment.server = d.server?.withSuffix("/")
        deployment.path = d.path
        deployment.maxSize = d.maxSize
    }

    /* Delete deployments that have been removed. */
    for (d in this.deployments.asSequence()) {
        if (deployments.none { it.name == d.name }) {
            d.delete()
        }
    }
}