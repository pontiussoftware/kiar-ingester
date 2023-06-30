package ch.pontius.kiar.api.routes.job

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobStatus
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.util.findById
import java.nio.ByteBuffer
import java.nio.file.Files

@OpenApi(
    path = "/api/jobs/{id}/upload",
    methods = [HttpMethod.POST],
    summary = "Uploads a KIAR for the given job.",
    operationId = "uploadKiar",
    tags = ["Job"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the Job for which a KIAR should be uploaded.", required = true)
    ],
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
            throw ErrorStatusException(400, "Job with ID $jobId is in wrong status for KIAR upload.")
        }

        job.template?.participant?.name ?: throw ErrorStatusException(400, "Jov with ID $jobId is not associated with a proper participant.")
    }

    /* Upload file. */
    val file = ctx.uploadedFile("kiar") ?: throw ErrorStatusException(400, "KIAR file is missing from upload.")
    file.content().use { input ->
        Files.newOutputStream(config.ingestPath.resolve(participant).resolve("$jobId.kiar")).use { output ->
            val buffer = ByteArray(10_000_000) /* 10 MB buffer. */
            var read = input.read(buffer)
            if (read > -1) {
                throw ErrorStatusException(400, "Cannot upload empty file.")
            }
            if (ByteBuffer.wrap(buffer).int != 0x04034b50) { /* Look for ZIP header. */
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
    store.transactional(true) {
        val job = try {
            DbJob.findById(jobId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Job with ID $jobId could not be found.")
        }
        job.status = DbJobStatus.HARVESTED
    }

    /* Return success. */
    ctx.json(SuccessStatus("KIAR file ${file.filename()} uploaded successfully."))
}