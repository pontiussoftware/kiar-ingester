package ch.pontius.kiar.api.routes.job

import ch.pontius.kiar.api.model.job.*
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.jobs.JobLogs
import ch.pontius.kiar.database.jobs.JobLogs.toJobLog
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.database.jobs.Jobs.toJob
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.utilities.extensions.currentUser
import ch.pontius.kiar.utilities.extensions.parseBodyOrThrow
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

@OpenApi(
    path = "/api/jobs/active",
    methods = [HttpMethod.GET],
    summary = "Retrieves all jobs that are currently active. Non-administrator users can only see Jobs that belong to them.",
    operationId = "getActiveJobs",
    tags = ["Job"],
    pathParams = [],
    queryParams = [
        OpenApiParam(name = "page", type = Int::class, description = "The page index (zero-based) for pagination.", required = false),
        OpenApiParam(name = "pageSize", type = Int::class, description = "The page size for pagination.", required = false)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(PaginatedJobResult::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getActiveJobs(ctx: Context, server: IngesterServer) {
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50

    /* Fetch jobs. */
    val (count, results) = transaction {
        val currentUser = ctx.currentUser()
        val query = (Jobs innerJoin JobTemplates innerJoin Participants).selectAll().where {
            Jobs.status inList listOf(JobStatus.CREATED, JobStatus.HARVESTED, JobStatus.RUNNING, JobStatus.INTERRUPTED, JobStatus.SCHEDULED)
        }

        if (currentUser.role == Role.MANAGER || currentUser.role == Role.VIEWER) {
            if (currentUser.institution == null) {
                query.andWhere { Op.FALSE }
            } else {
                query.andWhere {
                    Participants.id inSubQuery Institutions.select(Institutions.participantId)
                        .where { Institutions.name eq currentUser.institution.name }
                }
            }
        }

        query.count() to query.orderBy(Jobs.modified, SortOrder.DESC).offset((page * pageSize).toLong()).limit(pageSize).map {
            val job = it.toJob()
            val context = server.getContext(job.id!!)
            if (context != null) {
                job.processed = context.processed
                job.skipped = context.skipped
                job.error = context.error
            }
            job
        }
    }

    /* Return results. */
    ctx.json(PaginatedJobResult(count, page, pageSize, results))
}

@OpenApi(
    path = "/api/jobs/inactive",
    methods = [HttpMethod.GET],
    summary = "Retrieves all jobs that are currently inactive (job history). Non-administrator users can only see Jobs that belong to them.",
    operationId = "getInactiveJobs",
    queryParams = [
        OpenApiParam(name = "page", type = Int::class, description = "The page index (zero-based) for pagination.", required = false),
        OpenApiParam(name = "pageSize", type = Int::class, description = "The page size for pagination.", required = false)
    ],
    tags = ["Job"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(PaginatedJobResult::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getInactiveJobs(ctx: Context) {
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50

    /* Fetch jobs. */
    val (count, results) = transaction {
        val currentUser = ctx.currentUser()
        val query = (Jobs innerJoin JobTemplates innerJoin Participants).selectAll().where {
            Jobs.status inList listOf(JobStatus.ABORTED, JobStatus.FAILED, JobStatus.INGESTED)
        }

        if (currentUser.role == Role.MANAGER || currentUser.role == Role.VIEWER) {
            if (currentUser.institution == null) {
                query.andWhere { Op.FALSE }
            } else {
                query.andWhere {
                    Participants.id inSubQuery Institutions.select(Institutions.participantId)
                        .where { Institutions.name eq currentUser.institution.name }
                }
            }
        }

        query.count() to query.orderBy(Jobs.modified, SortOrder.DESC).offset((page * pageSize).toLong()).limit(pageSize).map {
            val job = it.toJob()
            val count = JobLogs.selectAll().where { JobLogs.jobId eq job.id!! }.count()
            job.copy(logEntries = count)
        }
    }

    /* Return results. */
    ctx.json(PaginatedJobResult(count, page, pageSize, results))
}

@OpenApi(
    path = "/api/jobs/{id}/logs",
    methods = [HttpMethod.GET],
    summary = "Retrieves the job log for the provided job ID.",
    operationId = "getJobLog",
    tags = ["Job"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the Job for which the logs should be retrieved.", required = true)
    ],
    queryParams = [
        OpenApiParam(name = "page", type = Int::class, description = "The page index (zero-based) for pagination.", required = false),
        OpenApiParam(name = "pageSize", type = Int::class, description = "The page size  for pagination.", required = false) ,
        OpenApiParam(name = "level", type = String::class, description = "A filter for the 'level' field.", required = false),
        OpenApiParam(name = "context", type = String::class, description = "A filter for the 'context' field.", required = false),
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(PaginatedJobLogResult::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getJobLogs(ctx: Context) {
    val jobId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50
    val level = ctx.queryParam("level")?.uppercase()?.let { JobLogLevel.valueOf(it) }
    val context = ctx.queryParam("context")?.uppercase()?.let { JobLogContext.valueOf(it) }

    /* Fetch job logs. */
    val (count, results) = transaction {
        val query = JobLogs.selectAll().where { JobLogs.jobId eq jobId }

        /* Apply filters (optional). */
        if (level != null) {
            query.andWhere { JobLogs.level eq level }
        }
        if (context != null) {
            query.andWhere { JobLogs.context eq context }
        }

        query.count() to query.offset((page * pageSize).toLong()).limit(pageSize).map { it.toJobLog() }
    }

    /* Return results. */
    ctx.json(PaginatedJobLogResult(count, page, pageSize, results))
}

@OpenApi(
    path = "/api/jobs/{id}/logs",
    methods = [HttpMethod.DELETE],
    summary = "Purges the logs for the job with the provided ID.",
    operationId = "deletePurgeJobLog",
    tags = ["Job"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the Job for which the logs should be pruged.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(PaginatedJobLogResult::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun purgeJobLogs(ctx: Context) {
    val jobId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")
    val deleted = transaction {
        JobLogs.deleteWhere { JobLogs.jobId eq jobId }
    }
    ctx.json(SuccessStatus("Logs for job $jobId purged successfully (count = $deleted)."))
}

@OpenApi(
    path = "/api/jobs",
    methods = [HttpMethod.POST],
    summary = "Creates a new job.",
    operationId = "postCreateJob",
    tags = ["Job"],
    requestBody = OpenApiRequestBody([OpenApiContent(CreateJobRequest::class)], required = true),
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Job::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun createJob(ctx: Context) {
    val request = ctx.parseBodyOrThrow<CreateJobRequest>()

    /* Create new job. */
    val created = transaction {
        val currentUser = ctx.currentUser()
        val template = JobTemplates.getById(request.templateId)
            ?: throw ErrorStatusException(404, "Job template with ID ${request.templateId} could not be found.")

        /* Check if user's participant is the same as the one associated with the template. */
        if (currentUser.role != Role.ADMINISTRATOR) {
            if (currentUser.institution == null) {
                throw ErrorStatusException(403, "You are not allowed to create a job for template ${template.id}.")
            }
            val participant = (Institutions innerJoin Participants)
                .select(Participants.name)
                .where { Institutions.name eq currentUser.institution.name }.map { it[Participants.name] }.firstOrNull()
            if (participant != template.participantName) {
                throw ErrorStatusException(403, "You are not allowed to create a job for template ${template.id}.")
            }
        }

        /* Create job. */
        val jobId = Jobs.insertAndGetId { insert ->
            insert[name] = if (request.jobName.isNullOrEmpty()) { (template.name + "-${System.currentTimeMillis()}") } else { request.jobName }
            insert[templateId] = template.id
            insert[src] = JobSource.WEB
            insert[status] = JobStatus.CREATED
            insert[createdBy] = currentUser.username
        }.value

        /* Return created object. */
        Job(
            id = jobId,
            name =  if (request.jobName.isNullOrEmpty()) { (template.name + "-${System.currentTimeMillis()}") } else { request.jobName },
            template = template,
            source =  JobSource.WEB,
            status = JobStatus.CREATED,
            createdAt = Instant.now().toEpochMilli(),
            createdBy = currentUser.username,
        )
    }

    /* Return job object. */
    ctx.json(created)
}