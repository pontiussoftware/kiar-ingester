package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.api.model.config.transformers.TransformerConfig
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.routes.session.currentUser
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.config.jobs.DbJobType
import ch.pontius.kiar.database.config.mapping.DbEntityMapping
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.database.config.transformers.DbTransformer
import ch.pontius.kiar.database.config.transformers.DbTransformerParameter
import ch.pontius.kiar.database.institution.DbParticipant
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import kotlinx.dnq.util.findById

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
fun listJobTemplates(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        val user = ctx.currentUser()
        val templates = if (user.role == DbRole.ADMINISTRATOR) {
            DbJobTemplate.all()
        } else if (user.institution?.participant != null) {
            DbJobTemplate.filter { it.participant eq user.institution?.participant }
        } else {
            DbJobTemplate.emptyQuery()
        }
        ctx.json(templates.mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/templates/types",
    methods = [HttpMethod.GET],
    summary = "Lists all available job template types.",
    operationId = "getListJobTemplateTypes",
    tags = ["Config", "Job Template"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<JobType>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listJobTemplateTypes(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        ctx.json(DbJobType.all().mapToArray { it.toApi() })
    }
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
fun createJobTemplate(ctx: Context, store: TransientEntityStore, server: IngesterServer) {
    val request = try {
        ctx.bodyAsClass(JobTemplate::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }

    val created = store.transactional {
        /* Update basic properties. */
        val mapping = DbJobTemplate.new {
            name = request.name
            description = request.description
            type = request.type.toDb()
            startAutomatically = request.startAutomatically
            participant = DbParticipant.filter { it.name eq request.participantName }.firstOrNull()
                ?: throw ErrorStatusException(404, "Could not find participant with name ${request.participantName}.")
            solr = DbSolr.filter { it.name eq request.solrConfigName }.firstOrNull()
                ?: throw ErrorStatusException(404, "Could not find Apache Solr configuration with name ${request.solrConfigName}.")
            mapping = DbEntityMapping.filter { it.name eq request.entityMappingName }.firstOrNull()
                ?: throw ErrorStatusException(404, "Could not find entity mapping configuration with name ${request.entityMappingName}.")

            /* Adds all transformer configuration to template. */
            this.merge(request.transformers)
        }

        /* Now merge attribute mappings. */
        mapping.toApi() to mapping.sourcePath(server.config)
    }

    /* Schedule a file watcher. */
    if (created.first.startAutomatically) {
        server.scheduleWatcher(created.first.name, created.second)
    }

    ctx.json(created.first)
}

@OpenApi(
    path = "/api/templates/{id}",
    methods = [HttpMethod.GET],
    summary = "Deletes an existing job template.",
    operationId = "getJobTemplate",
    tags = ["Config", "Job Template"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the job template to retrieve.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(JobTemplate::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getJobTemplate(ctx: Context, store: TransientEntityStore) {
    val templateId = ctx.pathParam("id")
    val template = store.transactional {
        try {
            DbJobTemplate.findById(templateId).toApi(true)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job template with ID $templateId could not be found.")
        }
    }
    ctx.json(template)
}

@OpenApi(
    path = "/api/templates/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing job template.",
    operationId = "deleteJobTemplate",
    tags = ["Config", "Job Template"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the job template that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteJobTemplate(ctx: Context, store: TransientEntityStore, server: IngesterServer) {
    val templateId = ctx.pathParam("id")
    var terminateWatcher = false
    val templateName = store.transactional {
        val template = try {
            DbJobTemplate.findById(templateId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job template with ID $templateId could not be found.")
        }

        /* If template was started automatically, watcher must be terminated. */
        if (template.startAutomatically) {
            terminateWatcher = true
        }
        val name = template.name
        template.delete()
        name
    }

    /* Terminate watcher if necessary. */
    if (terminateWatcher) {
        server.terminateWatcher(templateName)
    }

    ctx.json(SuccessStatus("Job template '$templateName' (id: $templateId) deleted successfully."))
}

@OpenApi(
    path = "/api/templates/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing job template.",
    operationId = "updateJobTemplate",
    tags = ["Config", "Job Template"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the job template that should be updated.", required = true)
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
fun updateJobTemplate(ctx: Context, store: TransientEntityStore) {
    /* Extract the ID and the request body. */
    val templateId = ctx.pathParam("id")
    val request = try {
        ctx.bodyAsClass(JobTemplate::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }

    /* Start transaction. */
    val updated = store.transactional {
        val template = try {
            DbJobTemplate.findById(templateId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job template with ID $templateId could not be found.")
        }

        /* Update basic properties. */
        template.name = request.name
        template. description = request.description
        template.type = request.type.toDb()
        template.startAutomatically = request.startAutomatically
        template.participant = DbParticipant.filter { it.name eq request.participantName }.firstOrNull()
            ?: throw ErrorStatusException(404, "Could not find participant with name ${request.participantName}.")
        template.solr = DbSolr.filter { it.name eq request.solrConfigName }.firstOrNull()
            ?: throw ErrorStatusException(404, "Could not find Apache Solr configuration with name ${request.solrConfigName}.")
        template.mapping = DbEntityMapping.filter { it.name eq request.solrConfigName }.firstOrNull()
            ?: throw ErrorStatusException(404, "Could not find entity mapping configuration with name ${request.entityMappingName}.")

        /* Now merge transformers. */
        template.merge(request.transformers)
        template.toApi()
    }

    ctx.json(updated)
}

/**
 * Overrides a [DbJobTemplate]'s [DbTransformer]s using the provided list.
 *
 * @param transformers [List] of [TransformerConfig]s to merge [DbJobTemplate] with.
 */
private fun DbJobTemplate.merge(transformers: List<TransformerConfig>) {
    this.transformers.clear()
    for (t in transformers) {
        this.transformers.add(DbTransformer.new {
            type = t.type.toDb()
            for (p in t.parameters) {
                parameters.add(DbTransformerParameter.new {
                    key = p.key
                    value = p.value
                })
            }
        })
    }
}