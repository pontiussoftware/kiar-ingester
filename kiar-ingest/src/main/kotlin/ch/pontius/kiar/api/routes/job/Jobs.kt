package ch.pontius.kiar.api.routes.job

import ch.pontius.kiar.api.model.job.*
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.openapi.*
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.jobs.JobLogs
import ch.pontius.kiar.database.jobs.JobLogs.toJobLog
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.database.jobs.Jobs.toJob
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.utilities.extensions.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

val getActiveJobsDoc: RouteDoc = {
    operationId = "getActiveJobs"
    summary = "Retrieves all jobs that are currently active. Non-administrator users can only see Jobs that belong to them."
    tags("Job")
    parameters {
        queryParam("page", "The page index (zero-based) for pagination.", INT32)
        queryParam("pageSize", "The page size for pagination.", INT32)
    }
    responses {
        json<PaginatedJobResult>(200)
        errors(401, 403, 500)
    }
}

suspend fun getActiveJobs(call: ApplicationCall, server: IngesterServer) {
    val page = call.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = call.queryParam("pageSize")?.toIntOrNull() ?: 50

    /* Fetch jobs. */
    val (count, results) = transaction {
        val currentUser = call.currentUser()
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
    call.respond(PaginatedJobResult(count, page, pageSize, results))
}

val getInactiveJobsDoc: RouteDoc = {
    operationId = "getInactiveJobs"
    summary = "Retrieves all jobs that are currently inactive (job history). Non-administrator users can only see Jobs that belong to them."
    tags("Job")
    parameters {
        queryParam("page", "The page index (zero-based) for pagination.", INT32)
        queryParam("pageSize", "The page size for pagination.", INT32)
    }
    responses {
        json<PaginatedJobResult>(200)
        errors(401, 403, 500)
    }
}

suspend fun getInactiveJobs(call: ApplicationCall) {
    val page = call.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = call.queryParam("pageSize")?.toIntOrNull() ?: 50

    /* Fetch jobs. */
    val (count, results) = transaction {
        val currentUser = call.currentUser()
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
    call.respond(PaginatedJobResult(count, page, pageSize, results))
}

val getJobLogsDoc: RouteDoc = {
    operationId = "getJobLog"
    summary = "Retrieves the job log for the provided job ID."
    tags("Job")
    parameters {
        pathParam("id", "The ID of the Job for which the logs should be retrieved.")
        queryParam("page", "The page index (zero-based) for pagination.", INT32)
        queryParam("pageSize", "The page size  for pagination.", INT32)
        queryParam("level", "A filter for the 'level' field.")
        queryParam("context", "A filter for the 'context' field.")
    }
    responses {
        json<PaginatedJobLogResult>(200)
        errors(401, 403, 500)
    }
}

suspend fun getJobLogs(call: ApplicationCall) {
    val jobId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")
    val page = call.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = call.queryParam("pageSize")?.toIntOrNull() ?: 50
    val level = call.queryParam("level")?.uppercase()?.let { value -> JobLogLevel.entries.find { it.name == value } ?: throw ErrorStatusException(400, "Unknown log level '$value'.") }
    val context = call.queryParam("context")?.uppercase()?.let { value -> JobLogContext.entries.find { it.name == value } ?: throw ErrorStatusException(400, "Unknown log context '$value'.") }

    /* Fetch job logs. */
    val (count, results) = transaction {
        val job = Jobs.getById(jobId) ?: throw ErrorStatusException(404, "Job with ID $jobId could not be found.")
        call.currentUser().requireParticipant(job.template?.participantName, "You are not allowed to access the logs of a job that has been created for another participant.")

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
    call.respond(PaginatedJobLogResult(count, page, pageSize, results))
}

val purgeJobLogsDoc: RouteDoc = {
    operationId = "deletePurgeJobLog"
    summary = "Purges the logs for the job with the provided ID."
    tags("Job")
    parameters {
        pathParam("id", "The ID of the Job for which the logs should be pruged.")
    }
    responses {
        json<PaginatedJobLogResult>(200)
        errors(401, 403, 500)
    }
}

suspend fun purgeJobLogs(call: ApplicationCall) {
    val jobId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")
    val deleted = transaction {
        val job = Jobs.getById(jobId) ?: throw ErrorStatusException(404, "Job with ID $jobId could not be found.")
        call.currentUser().requireParticipant(job.template?.participantName, "You are not allowed to purge the logs of a job that has been created for another participant.")
        JobLogs.deleteWhere { JobLogs.jobId eq jobId }
    }
    call.respond(SuccessStatus("Logs for job $jobId purged successfully (count = $deleted)."))
}

val createJobDoc: RouteDoc = {
    operationId = "postCreateJob"
    summary = "Creates a new job."
    tags("Job")
    jsonBody<CreateJobRequest>()
    responses {
        json<Job>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun createJob(call: ApplicationCall) {
    val request = call.receiveOrThrow<CreateJobRequest>()

    /* Create new job. */
    val created = transaction {
        val currentUser = call.currentUser()
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
    call.respond(created)
}