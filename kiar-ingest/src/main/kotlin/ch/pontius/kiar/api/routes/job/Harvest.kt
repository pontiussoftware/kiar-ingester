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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.StandardOpenOption


@OpenApi(
    path = "/api/jobs/{id}/upload",
    methods = [HttpMethod.PUT],
    summary = "Uploads a KIAR for the given job.",
    operationId = "putUploadKiar",
    tags = ["Job"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the Job for which a KIAR should be uploaded.", required = true)
    ],
    requestBody = OpenApiRequestBody(content = [
        OpenApiContent(mimeType = ContentType.FORM_DATA_MULTIPART, properties = [OpenApiContentProperty(name = "kiar", type = "string", format = "binary")])
   ], description = "The uploaded KIAR file.", required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun uploadKiar(ctx: Context, store: TransientEntityStore, config: Config) {
    /* Obtain and check Job. */
    val jobId = ctx.pathParam("id")
    val participant = store.transactional(false) {
        val job = try {
            DbJob.findById(jobId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job with ID $jobId could not be found.")
        }

        if (job.status != DbJobStatus.CREATED) {
            throw ErrorStatusException(400, "Job with ID $jobId is in wrong state.")
        }

        job.template?.participant?.name ?: throw ErrorStatusException(400, "Jov with ID $jobId is not associated with a proper participant.")
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

    /* Upload the first file. */
    upload.next().inputStream.use { input ->
        Files.newOutputStream(ingestPath.resolve(jobId), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use { output ->
            val buffer = ByteArray(25_000_000) /* 25 MB buffer. */
            var read = input.read(buffer)
            if (read == -1) {
                throw ErrorStatusException(400, "Cannot upload empty file.")
            }
            if (ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).int != 0x04034b50) { /* Look for ZIP header. */
                throw ErrorStatusException(400, "Uploaded file could not be verified to be a valid KIAR file.")
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
        job.status = DbJobStatus.HARVESTED
        job.changedAt = DateTime.now()
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
        if (job.status != DbJobStatus.HARVESTED) {
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