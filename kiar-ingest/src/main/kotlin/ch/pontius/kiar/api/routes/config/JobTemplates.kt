package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import ch.pontius.kiar.api.model.config.transformers.TransformerConfig
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.database.config.EntityMappings
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.config.JobTemplates.toJobTemplate
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.Transformers
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.utilities.extensions.currentUser
import ch.pontius.kiar.utilities.extensions.parseBodyOrThrow
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

@OpenApi(
    path = "/api/templates",
    methods = [HttpMethod.GET],
    summary = "Lists all available job templates.",
    operationId = "getListJobTemplates",
    tags = ["Config", "Job Template", "Job"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<JobTemplate>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listJobTemplates(ctx: Context) {
    val templates = transaction {
        val user = ctx.currentUser()
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
    ctx.json(templates.toTypedArray())
}

@OpenApi(
    path = "/api/templates",
    methods = [HttpMethod.POST],
    summary = "Creates a new job template.",
    operationId = "postCreateJobTemplate",
    tags = ["Config", "Job Template"],
    pathParams = [],
    requestBody = OpenApiRequestBody([OpenApiContent(JobTemplate::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(JobTemplate::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun createJobTemplate(ctx: Context, server: IngesterServer) {
    val request = ctx.parseBodyOrThrow<JobTemplate>()
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
        request.copy(jobTemplateId)
    }

    /* Schedule watcher if job template starts automatically. */
    if (created.startAutomatically) {
        server.scheduleWatcher(created.id!!, created.sourcePath(server.config))
    }

    /* Return created JSON. */
    ctx.json(created)
}

@OpenApi(
    path = "/api/templates/{id}",
    methods = [HttpMethod.GET],
    summary = "Deletes an existing job template.",
    operationId = "getJobTemplate",
    tags = ["Config", "Job Template"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the job template to retrieve.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(JobTemplate::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getJobTemplate(ctx: Context) {
    val templateId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job template ID.")
    val template = transaction {
        val template =  JobTemplates.getById(templateId) ?: throw ErrorStatusException(404, "Job template with ID $templateId could not be found.")
        val transformers = Transformers.getByJobTemplateId(templateId)
        template.copy(transformers = transformers)
    }
    ctx.json(template)
}

@OpenApi(
    path = "/api/templates/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing job template.",
    operationId = "updateJobTemplate",
    tags = ["Config", "Job Template"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the job template that should be updated.", required = true)
    ],
    requestBody = OpenApiRequestBody([OpenApiContent(JobTemplate::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(JobTemplate::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun updateJobTemplate(ctx: Context, server: IngesterServer) {
    /* Extract the ID and the request body. */
    val templateId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job template ID.")
    val request = ctx.parseBodyOrThrow<JobTemplate>()

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
    ctx.json(request)
}

@OpenApi(
    path = "/api/templates/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing job template.",
    operationId = "deleteJobTemplate",
    tags = ["Config", "Job Template"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the job template that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteJobTemplate(ctx: Context, server: IngesterServer) {
    val templateId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job template ID.")
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
        ctx.json(SuccessStatus("Job template with ID $templateId  deleted successfully."))
    } else {
        ctx.json(ErrorStatus(404, "Job template with ID $templateId could not be deleted because it could not be found."))
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