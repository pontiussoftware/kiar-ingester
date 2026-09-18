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
import ch.pontius.kiar.utilities.extensions.receiveOrThrow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import io.ktor.server.application.ApplicationCall
import ch.pontius.kiar.api.openapi.*
import io.ktor.server.response.respond
import ch.pontius.kiar.utilities.extensions.pathParam

val listEntityMappingsDoc: RouteDoc = {
    operationId = "getListEntityMappings"
    summary = "Lists all available entity mappings."
    tags("Config", "Entity Mapping")
    responses {
        json<List<EntityMapping>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listEntityMappings(call: ApplicationCall) {
    val mappings = transaction {
        EntityMappings.selectAll().map { it.toEntityMapping() }
    }
    call.respond(mappings)
}

val createEntityMappingDoc: RouteDoc = {
    operationId = "postCreateEntityMapping"
    summary = "Creates a new entity mapping."
    tags("Config", "Entity Mapping")
    jsonBody<EntityMapping>()
    responses {
        json<EntityMapping>(200)
        errors(400, 401, 403, 500)
    }
}

suspend fun createEntityMapping(call: ApplicationCall) {
    val request = call.receiveOrThrow<EntityMapping>()
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
    call.respond(created)
}

val getEntityMappingDoc: RouteDoc = {
    operationId = "getEntityMapping"
    summary = "Retrieves all the details about an entity mapping."
    tags("Config", "Entity Mapping")
    parameters {
        pathParam("id", "The ID of the entity mapping that should be retrieved.")
    }
    responses {
        json<EntityMapping>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun getEntityMapping(call: ApplicationCall) {
    val mappingId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed mapping ID.")
    val mapping = transaction {
        val mapping = EntityMappings.selectAll().where { EntityMappings.id eq mappingId }.map { it.toEntityMapping() }.firstOrNull()
            ?: throw ErrorStatusException(404, "Could not find entity mapping with ID $mappingId.")

        /* Fetch attribute mappings. */
        val attributeMappings = AttributeMappings.selectAll().where { AttributeMappings.entityMappingId eq mappingId }.map { it.toAttributeMapping() }

        /* Return copy. */
        mapping.copy(attributes = attributeMappings)
    }
    call.respond(mapping)
}

val updateEntityMappingDoc: RouteDoc = {
    operationId = "updateEntityMapping"
    summary = "Updates an existing entity mapping."
    tags("Config", "Entity Mapping")
    parameters {
        pathParam("id", "The ID of the entity mapping that should be updated.")
    }
    jsonBody<EntityMapping>()
    responses {
        json<EntityMapping>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun updateEntityMapping(call: ApplicationCall) {
    /* Extract the ID and the request body. */
    val mappingId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed mapping ID.")
    val request = call.receiveOrThrow<EntityMapping>()

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

    call.respond(request)
}

val deleteEntityMappingDoc: RouteDoc = {
    operationId = "deleteEntityMapping"
    summary = "Deletes an existing entity mapping."
    tags("Config", "Entity Mapping")
    parameters {
        pathParam("id", "The ID of the entity mapping that should be deleted.")
    }
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun deleteEntityMapping(call: ApplicationCall) {
    val mappingId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed mapping ID.")
    val deleted = transaction {
        EntityMappings.deleteWhere { EntityMappings.id eq mappingId }
    }
    if (deleted > 0) {
        call.respond(SuccessStatus("Mapping with ID $mappingId deleted successfully."))
    } else {
        call.respond(ErrorStatus(404, "Mapping with ID $mappingId could not be deleted, because it does not exist."))
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
            insert[AttributeMappings.entityMappingId] = entityMappingId
            insert[AttributeMappings.src] = a.source
            insert[AttributeMappings.destination] = a.destination
            insert[AttributeMappings.parser] = a.parser
            insert[AttributeMappings.required] = a.required
            insert[AttributeMappings.multiValued] = a.multiValued
            insert[AttributeMappings.parameters] = a.parameters
        }
    }
}