package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import ch.pontius.kiar.api.model.config.transformers.TransformerConfig
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.openapi.*
import ch.pontius.kiar.database.config.EntityMappings
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.config.JobTemplates.toJobTemplate
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.Transformers
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.utilities.extensions.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

val listJobTemplatesDoc: RouteDoc = {
    operationId = "getListJobTemplates"
    summary = "Lists all available job templates."
    tags("Config", "Job Template", "Job")
    responses {
        json<List<JobTemplate>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listJobTemplates(call: ApplicationCall) {
    val templates = transaction {
        val user = call.currentUser()
        val query = (JobTemplates innerJoin Participants innerJoin SolrConfigs innerJoin EntityMappings).selectAll()

        if (user.role != Role.ADMINISTRATOR) {
            val participantId = user.institution?.let {
                Institutions.select(Institutions.participantId)
                    .where { Institutions.name eq user.institution.name }
                    .map { it[Institutions.participantId].value }
                    .firstOrNull()
            }
            if (participantId != null) {
                query.andWhere { JobTemplates.participantId eq participantId }
            } else {
                return@transaction emptyList()
            }
        }

        query.orderBy(JobTemplates.name).map { it.toJobTemplate() }
    }

    /* Return results. */
    call.respond(templates)
}

val createJobTemplateDoc: RouteDoc = {
    operationId = "postCreateJobTemplate"
    summary = "Creates a new job template."
    tags("Config", "Job Template")
    jsonBody<JobTemplate>()
    responses {
        json<JobTemplate>(200)
        errors(400, 401, 403, 500)
    }
}

suspend fun createJobTemplate(call: ApplicationCall, server: IngesterServer) {
    val request = call.receiveOrThrow<JobTemplate>()
    request.name.requireSafePathSegment("job template name") /* The name becomes part of the watched file's path. */
    val created = transaction {
        val jobTemplateId = JobTemplates.insertAndGetId { insert ->
            insert[name] = request.name
            insert[description] = request.description
            insert[type] = request.type
            insert[startAutomatically] = request.startAutomatically
            insert[participantId] = Participants.idByName(request.participantName) ?: throw ErrorStatusException(404, "Could not find participant with name '${request.participantName}'.")
            request.config?.name?.apply {
                insert[solrId] = SolrConfigs.idByName(this) ?: throw ErrorStatusException(404, "Could not find Apache Solr configuration with name '$this'.")
            }
            request.mapping?.name?.apply {
                insert[entityMappingId] = EntityMappings.idByName(this) ?: throw ErrorStatusException(404, "Could not find Apache Solr configuration with name '$this'.")
            }
        }.value

        /* Save transformers. */
        saveTransformers(jobTemplateId, request.transformers)

        /* Return created items with ID. */
        request.copy(id = jobTemplateId)
    }

    /* Schedule watcher if job template starts automatically. */
    if (created.startAutomatically) {
        server.scheduleWatcher(created.id!!, created.sourcePath(server.config))
    }

    /* Return created JSON. */
    call.respond(created)
}

val getJobTemplateDoc: RouteDoc = {
    operationId = "getJobTemplate"
    summary = "Deletes an existing job template."
    tags("Config", "Job Template")
    parameters {
        pathParam("id", "The ID of the job template to retrieve.")
    }
    responses {
        json<JobTemplate>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun getJobTemplate(call: ApplicationCall) {
    val templateId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job template ID.")
    val template = transaction {
        val template =  JobTemplates.getById(templateId) ?: throw ErrorStatusException(404, "Job template with ID $templateId could not be found.")
        call.currentUser().requireParticipant(template.participantName, "Job template with ID $templateId cannot be accessed by current user.")
        val transformers = Transformers.getByJobTemplateId(templateId)
        template.copy(transformers = transformers)
    }
    call.respond(template)
}

val updateJobTemplateDoc: RouteDoc = {
    operationId = "updateJobTemplate"
    summary = "Updates an existing job template."
    tags("Config", "Job Template")
    parameters {
        pathParam("id", "The ID of the job template that should be updated.")
    }
    jsonBody<JobTemplate>()
    responses {
        json<JobTemplate>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun updateJobTemplate(call: ApplicationCall, server: IngesterServer) {
    /* Extract the ID and the request body. */
    val templateId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job template ID.")
    val request = call.receiveOrThrow<JobTemplate>()
    request.name.requireSafePathSegment("job template name") /* The name becomes part of the watched file's path. */

    /* Start transaction. */
    transaction {
        val existing = JobTemplates.getById(templateId) ?: throw ErrorStatusException(404, "Could not update job template with ID $templateId because it could not be found.")

        /* Perform update. */
        JobTemplates.update({ JobTemplates.id eq templateId }) { update ->
            update[name] = request.name
            update[description] = request.description
            update[type] = request.type
            update[startAutomatically] = request.startAutomatically
            update[participantId] = Participants.idByName(request.participantName) ?: throw ErrorStatusException(404, "Could not find participant with name '${request.participantName}'.")
            request.config?.name?.apply {
                update[solrId] = SolrConfigs.idByName(this) ?: throw ErrorStatusException(404, "Could not find Apache Solr configuration with name '$this'.")
            }
            request.mapping?.name?.apply {
                update[entityMappingId] = EntityMappings.idByName(this) ?: throw ErrorStatusException(404, "Could not find Apache Solr configuration with name '$this'.")
            }
            update[modified] = Instant.now()
        }

        /* Saves all transformers. */
        saveTransformers(templateId, request.transformers)

        /* (De-)Schedule watchers. */
        if (existing.startAutomatically && !request.startAutomatically) {
            server.terminateWatcher(templateId)
        } else if (!existing.startAutomatically && request.startAutomatically) {
            server.scheduleWatcher(templateId, request.sourcePath(server.config))
        }
    }

    /* Returns updated object. */
    call.respond(request)
}

val deleteJobTemplateDoc: RouteDoc = {
    operationId = "deleteJobTemplate"
    summary = "Deletes an existing job template."
    tags("Config", "Job Template")
    parameters {
        pathParam("id", "The ID of the job template that should be deleted.")
    }
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun deleteJobTemplate(call: ApplicationCall, server: IngesterServer) {
    val templateId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job template ID.")
    var terminateWatcher = false
    val deleted = transaction {
        terminateWatcher = JobTemplates.select(JobTemplates.startAutomatically).where { JobTemplates.id eq templateId }.map { it[JobTemplates.startAutomatically] }.firstOrNull() ?: false
        JobTemplates.deleteWhere { JobTemplates.id eq templateId }
    }

    /* Terminate watcher if necessary. */
    if (terminateWatcher) {
        server.terminateWatcher(templateId)
    }

    /* Return status. */
    if (deleted > 0) {
        call.respond(SuccessStatus("Job template with ID $templateId  deleted successfully."))
    } else {
        call.respond(ErrorStatus(404, "Job template with ID $templateId could not be deleted because it could not be found."))
    }
}

/**
 * Overrides a [JobTemplate]'s [TransformerConfig]s using the provided list.
 *
 * @param jobTemplateId The [JobTemplateId] to store [TransformerConfig]s for.
 * @param transformers [List] of [TransformerConfig]s to store.
 */
private fun saveTransformers(jobTemplateId: JobTemplateId, transformers: List<TransformerConfig>) {
    Transformers.deleteWhere { Transformers.jobTemplateId eq jobTemplateId }
    for ((i, t) in transformers.withIndex()) {
        Transformers.insert { insert ->
            insert[Transformers.jobTemplateId] = jobTemplateId
            insert[type] = t.type
            insert[order] = i
            insert[parameters] = t.parameters
        }
    }
}