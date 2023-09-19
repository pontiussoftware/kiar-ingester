import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.api.model.config.mappings.MappingFormat
import ch.pontius.kiar.api.model.config.mappings.ValueParser
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.config.mapping.*
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.util.findById
import org.joda.time.DateTime

@OpenApi(
    path = "/api/mappings",
    methods = [HttpMethod.GET],
    summary = "Lists all available entity mappings.",
    operationId = "getListEntityMappings",
    tags = ["Config", "Entity Mapping"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<EntityMapping>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listEntityMappings(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        val mappings = DbEntityMapping.all()
        ctx.json(mappings.mapToArray { it.toApiNoAttributes() })
    }
}

@OpenApi(
    path = "/api/mappings/parsers",
    methods = [HttpMethod.GET],
    summary = "Lists all available parses available for entity mapping.",
    operationId = "getListParsers",
    tags = ["Config", "Entity Mapping"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<ValueParser>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listParsers(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        val parsers = DbParser.filter { it.name ne "IMAGE" }
        ctx.json(parsers.mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/mappings/formats",
    methods = [HttpMethod.GET],
    summary = "Lists all available entity mapping formats.",
    operationId = "getListMappingFormats",
    tags = ["Config", "Entity Mapping"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<MappingFormat>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listMappingFormats(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        val parsers = DbFormat.all()
        ctx.json(parsers.mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/mappings",
    methods = [HttpMethod.POST],
    summary = "Creates a new entity mapping.",
    operationId = "postCreateEntityMapping",
    tags = ["Config", "Entity Mapping"],
    pathParams = [],
    requestBody = OpenApiRequestBody([OpenApiContent(EntityMapping::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(EntityMapping::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun createEntityMapping(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(EntityMapping::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }
    val created = store.transactional {
        /* Update basic properties. */
        val mapping = DbEntityMapping.new {
            name = request.name
            description = request.description
            type = request.type.toDb()
            createdAt = DateTime.now()
            changedAt = DateTime.now()
        }

        /* Now merge attribute mappings. */
        mapping.merge(request.attributes)
        mapping.toApi()
    }
    ctx.json(created)
}

@OpenApi(
    path = "/api/mappings/{id}",
    methods = [HttpMethod.GET],
    summary = "Retrieves all the details about an entity mapping.",
    operationId = "getEntityMapping",
    tags = ["Config", "Entity Mapping"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the entity mapping that should be retrieved.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(EntityMapping::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getEntityMapping(ctx: Context, store: TransientEntityStore) {
    val mappingId = ctx.pathParam("id")
    val mapping = store.transactional(true) {
        try {
            DbEntityMapping.findById(mappingId).toApi()
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Entity mapping with ID $mappingId could not be found.")
        }
    }
    ctx.json(mapping)
}


@OpenApi(
    path = "/api/mappings/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing entity mapping.",
    operationId = "deleteEntityMapping",
    tags = ["Config", "Entity Mapping"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the entity mapping that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteEntityMapping(ctx: Context, store: TransientEntityStore) {
    val mappingId = ctx.pathParam("id")
    store.transactional {
        val mapping = try {
            DbEntityMapping.findById(mappingId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Entity mapping with ID $mappingId could not be found.")
        }
        mapping.delete()
    }
    ctx.json(SuccessStatus("Mapping $mappingId deleted successfully."))
}

@OpenApi(
    path = "/api/mappings/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing entity mapping.",
    operationId = "updateEntityMapping",
    tags = ["Config", "Entity Mapping"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the entity mapping that should be updated.", required = true)
    ],
    requestBody = OpenApiRequestBody([OpenApiContent(EntityMapping::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(EntityMapping::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun updateEntityMapping(ctx: Context, store: TransientEntityStore) {
    /* Extract the ID and the request body. */
    val mappingId = ctx.pathParam("id")
    val request = try {
        ctx.bodyAsClass(EntityMapping::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }

    /* Start transaction. */
    val updated = store.transactional {
        val mapping = try {
            DbEntityMapping.findById(mappingId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Entity mapping with ID $mappingId could not be found.")
        }

        /* Update basic properties. */
        mapping.name = request.name
        mapping.description = request.description
        mapping.type = request.type.toDb()
        mapping.changedAt = DateTime.now()

        /* Now merge attribute mappings. */
        mapping.merge(request.attributes)
        mapping.toApi()
    }

    ctx.json(updated)
}

/**
 * Overrides a [DbEntityMapping]'s [DbAttributeMapping]s using the provided list.
 *
 * @param attributes [List] of [AttributeMapping]s to merge [DbEntityMapping] with.
 */
private fun DbEntityMapping.merge(attributes: List<AttributeMapping>) {
    /* Explicitly delete all existing attribute mappings (Note: this.attributes.clear() doesn't work). */
    for (a in this.attributes.asSequence()) {
        a.delete()
    }

    /* Re-add attributes. */
    for (a in attributes) {
        this.attributes.add(DbAttributeMapping.new {
            source = a.source
            destination = a.destination
            required = a.required
            multiValued = a.multiValued
            parser = a.parser.toDb()
            for (p in a.parameters) {
                parameters.add(DbAttributeMappingParameters.new { key = p.key; value = p.value })
            }
        })
    }
}