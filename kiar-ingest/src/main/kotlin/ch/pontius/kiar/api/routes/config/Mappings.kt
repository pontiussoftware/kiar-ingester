import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.api.model.config.mappings.EntityMappingId
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.config.AttributeMappings
import ch.pontius.kiar.database.config.AttributeMappings.toAttributeMapping
import ch.pontius.kiar.database.config.EntityMappings
import ch.pontius.kiar.database.config.EntityMappings.toEntityMapping
import ch.pontius.kiar.utilities.extensions.parseBodyOrThrow
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

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
fun listEntityMappings(ctx: Context) {
    val mappings = transaction {
        EntityMappings.selectAll().map { it.toEntityMapping() }
    }
    ctx.json(mappings.toTypedArray())
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
fun createEntityMapping(ctx: Context) {
    val request = ctx.parseBodyOrThrow<EntityMapping>()
    val created = transaction {
        val entityMappingId = EntityMappings.insertAndGetId { insert ->
            insert[name] = request.name
            insert[description] = request.description
            insert[format] = request.type
        }.value

        /* Now create attributes. */
        saveAttributeMappings(entityMappingId, request.attributes)

        /* Return copy of newly created mapping. */
        request.copy(id = entityMappingId)
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
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the entity mapping that should be retrieved.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(EntityMapping::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getEntityMapping(ctx: Context) {
    val mappingId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed mapping ID.")
    val mapping = transaction {
        val mapping = EntityMappings.selectAll().where { EntityMappings.id eq mappingId }.map { it.toEntityMapping() }.firstOrNull()
            ?: throw ErrorStatusException(404, "Could not find entity mapping with ID $mappingId.")

        /* Fetch attribute mappings. */
        val attributeMappings = AttributeMappings.selectAll().where { AttributeMappings.entityMappingId eq mappingId }.map { it.toAttributeMapping() }

        /* Return copy. */
        mapping.copy(attributes = attributeMappings)
    }
    ctx.json(mapping)
}

@OpenApi(
    path = "/api/mappings/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing entity mapping.",
    operationId = "updateEntityMapping",
    tags = ["Config", "Entity Mapping"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the entity mapping that should be updated.", required = true)
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
fun updateEntityMapping(ctx: Context) {
    /* Extract the ID and the request body. */
    val mappingId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed mapping ID.")
    val request = ctx.parseBodyOrThrow<EntityMapping>()

    /* Start transaction. */
    transaction {
        val updated = EntityMappings.update({ EntityMappings.id eq mappingId }) { update ->
            update[name] = request.name
            update[description] = request.description
            update[format] = request.type
            update[modified] = Instant.now()
        }

        /* Sanity check. */
        if (updated == 0) throw ErrorStatusException(404, "Entity mapping with ID $mappingId was not updated because it could not be found.")

        /* Save attributes. */
        saveAttributeMappings(mappingId, request.attributes)
    }

    ctx.json(request)
}

@OpenApi(
    path = "/api/mappings/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing entity mapping.",
    operationId = "deleteEntityMapping",
    tags = ["Config", "Entity Mapping"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the entity mapping that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteEntityMapping(ctx: Context) {
    val mappingId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed mapping ID.")
    val deleted = transaction {
        EntityMappings.deleteWhere { EntityMappings.id eq mappingId }
    }
    if (deleted > 0) {
        ctx.json(SuccessStatus("Mapping with ID $mappingId deleted successfully."))
    } else {
        ctx.json(ErrorStatus(404, "Mapping with ID $mappingId could not be deleted, because it does not exist."))
    }
}

/**
 * Overrides a [EntityMapping]'s [AttributeMapping]s using the provided list.
 *
 * @param entityMappingId The [EntityMappingId] of the [EntityMapping]
 * @param attributes [List] of [AttributeMapping]s to store.
 */
private fun saveAttributeMappings(entityMappingId: EntityMappingId, attributes: List<AttributeMapping>) {
    /* Delete all existing attribute mappings. */
    AttributeMappings.deleteWhere { AttributeMappings.entityMappingId eq entityMappingId }

    /* Re-add attributes. */
    for (a in attributes) {
        AttributeMappings.insert { insert ->
            insert[AttributeMappings.src] = a.source
            insert[AttributeMappings.destination] = a.destination
            insert[AttributeMappings.required] = a.required
            insert[AttributeMappings.multiValued] = a.multiValued
            insert[AttributeMappings.parameters] = a.parameters
        }
    }
}