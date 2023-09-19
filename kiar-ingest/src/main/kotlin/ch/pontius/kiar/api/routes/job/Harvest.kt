package ch.pontius.kiar.api.routes.job

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.routes.session.currentUser
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobStatus
import ch.pontius.kiar.ingester.IngesterServer
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.util.findById
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload
import org.joda.time.DateTime
import java.nio.file.Files
import java.nio.file.StandardOpenOption


@OpenApi(
    path = "/api/jobs/{id}/upload",
    methods = [HttpMethod.PUT],
    summary = "Uploads a file for the given job.",
    operationId = "putUpload",
    tags = ["Job"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the Job for which a file should be uploaded.", required = true)
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
fun upload(ctx: Context, store: TransientEntityStore, config: Config) {
    /* Obtain and check Job. */
    val jobId = ctx.pathParam("id")
    val first = ctx.queryParam("first")?.toBoolean() ?: false
    val last = ctx.queryParam("last")?.toBoolean() ?: false
    val participant = store.transactional(false) {
        val job = try {
            DbJob.findById(jobId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job with ID $jobId could not be found.")
        }

        if (job.status != DbJobStatus.CREATED && job.status != DbJobStatus.FAILED) {
            throw ErrorStatusException(400, "Job with ID $jobId is in wrong state.")
        }

        job.template?.participant?.name ?: throw ErrorStatusException(400, "Job with ID $jobId is not associated with a proper participant.")
    }

    /* Check for availability of directory and create it if necessary. */
    val ingestPath = config.ingestPath.resolve(participant)
    if (!Files.exists(ingestPath)) {
        Files.createDirectories(ingestPath)
    }

    /* Important: Streaming upload! */
    /* Make sure that one file has been uploaded. */
    val upload = JakartaServletFileUpload().getItemIterator(ctx.req())
    if (!upload.hasNext()) throw ErrorStatusException(401, "Uploaded file is missing.")

    /* Create or re-use output file. TODO: In case of an error, we need a way to recover here. */
    val outputStream = if (first) {
        Files.newOutputStream(ingestPath.resolve(jobId), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    } else {
        Files.newOutputStream(ingestPath.resolve(jobId), StandardOpenOption.APPEND, StandardOpenOption.WRITE)
    }

    /* Upload the first file. */
    outputStream.use { output ->
        upload.next().inputStream.use { input ->
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
    store.transactional {
        val job = try {
            DbJob.findById(jobId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job with ID $jobId could not be found.")
        }
        job.changedAt = DateTime.now()
        if (last) {
            job.status = DbJobStatus.HARVESTED
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
        OpenApiParam(name = "id", description = "The ID of the Job that should be started.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun scheduleJob(ctx: Context, store: TransientEntityStore, server: IngesterServer) {
    val jobId = ctx.pathParam("id")

    /* Perform sanity checks. */
    store.transactional(true) {
        val currentUser = ctx.currentUser()
        val job = try {
            DbJob.findById(jobId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job with ID $jobId could not be found.")
        }

        /* Check status of the job. */
        if (job.status != DbJobStatus.HARVESTED && job.status != DbJobStatus.FAILED && job.status != DbJobStatus.INTERRUPTED) {
            throw ErrorStatusException(400, "Job with ID $jobId is in wrong state.")
        }

        /* Check if user is actually allowed to start the job. */
        if (currentUser.role != DbRole.ADMINISTRATOR && job.template?.participant != currentUser.institution?.participant) {
            throw ErrorStatusException(403, "You are not allowed to start job $jobId.")
        }
    }

    /* Schedule job for execution. */
    server.scheduleJob(jobId)

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
        OpenApiParam(name = "id", description = "The ID of the Job that should be aborted.", required = true)
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
fun abortJob(ctx: Context, store: TransientEntityStore, server: IngesterServer) {
    val jobId = ctx.pathParam("id")
    store.transactional {
        val currentUser = ctx.currentUser()
        val job = try {
            DbJob.findById(jobId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job with ID $jobId could not be found.")
        }

        /* Check if user's participant is the same as the one associated with the template. */
        if (currentUser.role != DbRole.ADMINISTRATOR) {
            if (job.template?.participant != currentUser.institution?.participant) {
                throw ErrorStatusException(403, "You are not allowed to abort a job that has been created for another participant.")
            }
        }

        /* Check if job is still active. */
        if (!job.status.active) {
            throw ErrorStatusException(400, "Job with ID $jobId could not be aborted because it is already inactive.")
        }
        job.status = DbJobStatus.ABORTED
        job.changedAt = DateTime.now()
    }

    /* Inform ingest server that job should be terminated.*/
    if (!server.terminateJob(jobId)) {
        ctx.json(SuccessStatus("Successfully updated status of job $jobId."))
    } else {
        ctx.json(SuccessStatus("Successfully terminated job $jobId."))
    }
}