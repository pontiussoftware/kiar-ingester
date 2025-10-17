package ch.pontius.kiar.api.routes.job

import ch.pontius.kiar.api.model.job.JobStatus
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.utilities.extensions.currentUser
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant


@OpenApi(
    path = "/api/jobs/{id}/upload",
    methods = [HttpMethod.PUT],
    summary = "Uploads a file for the given job.",
    operationId = "putUpload",
    tags = ["Job"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the Job for which a file should be uploaded.", required = true)
    ],
    queryParams = [
        OpenApiParam(name = "first", description = "Set to 'true' if the submitted chunk is the first one.", required = false, type = Boolean::class),
        OpenApiParam(name = "last", description = "Set to 'true' if the submitted chunk is the last one.", required = false, type = Boolean::class)
    ],
    requestBody = OpenApiRequestBody(content = [
        OpenApiContent(mimeType = ContentType.FORM_DATA_MULTIPART, properties = [OpenApiContentProperty(name = "file", type = "string", format = "binary")])
   ], description = "The uploaded KIAR file.", required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun upload(ctx: Context, config: Config) {
    /* Obtain and check Job. */
    val jobId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")
    val first = ctx.queryParam("first")?.toBoolean() ?: false
    ctx.queryParam("last")?.toBoolean() ?: false
    val participant = transaction {
        val job = Jobs.getById(jobId) ?: throw ErrorStatusException(404, "Job with ID $jobId could not be found.")

        /* Check if job is still active. */
        if (job.status !in setOf(JobStatus.CREATED, JobStatus.FAILED)) {
            throw ErrorStatusException(400, "Job with ID $jobId is in wrong state.")
        }

        job.template?.participantName ?: throw ErrorStatusException(400, "Job with ID $jobId is not associated with a proper participant.")
    }

    /* Check for availability of directory and create it if necessary. */
    val ingestPath = config.ingestPath.resolve(participant)
    if (!Files.exists(ingestPath)) {
        Files.createDirectories(ingestPath)
    }

    /* Make sure that one file has been uploaded. */
    val upload = ctx.uploadedFiles("file").firstOrNull() ?: throw ErrorStatusException(401, "Uploaded file is missing.")

    /* Create or re-use output file. TODO: In case of an error, we need a way to recover here. */
    val outputStream = if (first) {
        Files.newOutputStream(ingestPath.resolve("$jobId.job"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    } else {
        Files.newOutputStream(ingestPath.resolve("$jobId.job"), StandardOpenOption.APPEND, StandardOpenOption.WRITE)
    }

    /* Upload the first file. */
    outputStream.use { output ->
        upload.content().use { input ->
            val buffer = ByteArray(5_000_000) /* 5 MB buffer. */
            var read = input.read(buffer)
            if (read == -1) {
                throw ErrorStatusException(400, "Cannot upload empty file.")
            }

            /* Start writing file to disk. */
            output.write(buffer, 0, read)
            do {
                read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
            } while (true)
        }
    }

    /* Update Job status. */
    transaction {
        Jobs.update({ Jobs.id eq jobId }) { update ->
            update[status] = JobStatus.HARVESTED
            update[modified] = Instant.now()

        }
    }

    /* Return success. */
    ctx.json(SuccessStatus("KIAR file uploaded successfully."))
}

@OpenApi(
    path = "/api/jobs/{id}/schedule",
    methods = [HttpMethod.PUT],
    summary = "Starts execution of a job.",
    operationId = "putScheduleJob",
    tags = ["Job"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the Job that should be started.", required = true)
    ],
    queryParams = [
        OpenApiParam(name = "test", type = Boolean::class, description = "True, if only a test-run should be executed.", required = false),
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun scheduleJob(ctx: Context, server: IngesterServer) {
    val jobId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")
    val test = ctx.queryParam("test")?.toBoolean() ?: false

    /* Perform sanity checks. */
    transaction {
        val currentUser = ctx.currentUser()
        val job = Jobs.getById(jobId) ?: throw ErrorStatusException(404, "Job with ID $jobId does not exist.")

        /* Check status of the job. */
        if (job.status !in setOf(JobStatus.HARVESTED, JobStatus.FAILED, JobStatus.INTERRUPTED)) {
            throw ErrorStatusException(400, "Job with ID $jobId is in wrong state.")
        }

        /* Check if user is actually allowed to start the job. */
        if (currentUser.role != Role.ADMINISTRATOR && job.template?.participantName != currentUser.institution?.participantName) {
            throw ErrorStatusException(403, "You are not allowed to start job $jobId.")
        }
    }

    /* Schedule job for execution. */
    server.scheduleJob(jobId, test)

    /* Return success. */
    ctx.json(SuccessStatus("Job $jobId scheduled successfully."))
}

@OpenApi(
    path = "/api/jobs/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Aborts a running job.",
    operationId = "deleteAbortJob",
    tags = ["Job"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the Job that should be aborted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun abortJob(ctx: Context, server: IngesterServer) {
    val jobId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")

    transaction {
        val currentUser = ctx.currentUser()
        val job = Jobs.getById(jobId) ?: throw ErrorStatusException(404, "Job with ID $jobId could not be found.")

        /* Check if user's participant is the same as the one associated with the template. */
        if (currentUser.role != Role.ADMINISTRATOR) {
            if (job.template?.participantName != currentUser.institution?.participantName) {
                throw ErrorStatusException(403, "You are not allowed to abort a job that has been created for another participant.")
            }
        }

        /* Check if job is still active. */
        if (job.status !in setOf(JobStatus.CREATED, JobStatus.HARVESTED, JobStatus.SCHEDULED, JobStatus.INGESTED, JobStatus.RUNNING)) {
            throw ErrorStatusException(400, "Job with ID $jobId could not be aborted because it is already inactive.")
        }
        Jobs.update({ Jobs.id eq jobId }) { update ->
            update[status] = JobStatus.ABORTED
            update[modified] = Instant.now()
        }
    }

    /* Inform ingest server that job should be terminated.*/
    if (!server.terminateJob(jobId)) {
        ctx.json(SuccessStatus("Successfully updated status of job $jobId."))
    } else {
        ctx.json(SuccessStatus("Successfully terminated job $jobId."))
    }
}