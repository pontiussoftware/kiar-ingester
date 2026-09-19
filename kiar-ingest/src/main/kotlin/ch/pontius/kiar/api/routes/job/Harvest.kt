package ch.pontius.kiar.api.routes.job

import ch.pontius.kiar.api.model.job.JobStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.openapi.*
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.utilities.extensions.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant


val uploadDoc: RouteDoc = {
    operationId = "putUpload"
    summary = "Uploads a file for the given job."
    tags("Job")
    parameters {
        pathParam("id", "The ID of the Job for which a file should be uploaded.")
        queryParam("first", "Set to 'true' if the submitted chunk is the first one.", BOOLEAN)
        queryParam("last", "Set to 'true' if the submitted chunk is the last one.", BOOLEAN)
    }
    multipartFile("file", "The uploaded KIAR file.")
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 500)
    }
}

suspend fun upload(call: ApplicationCall, config: Config) {
    /* Obtain and check Job. */
    val jobId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")
    val first = call.queryParam("first")?.toBoolean() ?: false
    val last = call.queryParam("last")?.toBoolean() ?: false
    val participant = transaction {
        val job = Jobs.getById(jobId) ?: throw ErrorStatusException(404, "Job with ID $jobId could not be found.")

        /* Check if user's participant is the same as the one associated with the job. */
        call.currentUser().requireParticipant(job.template?.participantName, "You are not allowed to upload data for a job that has been created for another participant.")

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
    val upload = call.uploadedFiles("file").firstOrNull() ?: throw ErrorStatusException(401, "Uploaded file is missing.")

    /* Create or re-use output file. TODO: In case of an error, we need a way to recover here. */
    val outputStream = if (first) {
        Files.newOutputStream(ingestPath.resolve("$jobId.job"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    } else {
        Files.newOutputStream(ingestPath.resolve("$jobId.job"), StandardOpenOption.APPEND, StandardOpenOption.WRITE)
    }

    /* Upload the first file. */
    try { outputStream.use { output ->
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
    } } finally {
        upload.delete()
    }

    /* Update Job status if this was the last chunk */
    if (last) {
        transaction {
            Jobs.update({ Jobs.id eq jobId }) { update ->
                update[status] = JobStatus.HARVESTED
                update[modified] = Instant.now()
            }
        }
    }

    /* Return success. */
    call.respond(SuccessStatus("KIAR file uploaded successfully."))
}

val scheduleJobDoc: RouteDoc = {
    operationId = "putScheduleJob"
    summary = "Starts execution of a job."
    tags("Job")
    parameters {
        pathParam("id", "The ID of the Job that should be started.")
        queryParam("test", "True, if only a test-run should be executed.", BOOLEAN)
    }
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 500)
    }
}

suspend fun scheduleJob(call: ApplicationCall, server: IngesterServer) {
    val jobId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")
    val test = call.queryParam("test")?.toBoolean() ?: false

    /* Perform sanity checks. */
    transaction {
        val currentUser = call.currentUser()
        val job = Jobs.getById(jobId) ?: throw ErrorStatusException(404, "Job with ID $jobId does not exist.")

        /* Check status of the job. */
        if (job.status !in setOf(JobStatus.HARVESTED, JobStatus.FAILED, JobStatus.INTERRUPTED)) {
            throw ErrorStatusException(400, "Job with ID $jobId is in wrong state.")
        }

        /* Check if user is actually allowed to start the job. */
        currentUser.requireParticipant(job.template?.participantName, "You are not allowed to start job $jobId.")
    }

    /* Schedule job for execution. */
    server.scheduleJob(jobId, test)

    /* Return success. */
    call.respond(SuccessStatus("Job $jobId scheduled successfully."))
}

val abortJobDoc: RouteDoc = {
    operationId = "deleteAbortJob"
    summary = "Aborts a running job."
    tags("Job")
    parameters {
        pathParam("id", "The ID of the Job that should be aborted.")
    }
    responses {
        json<SuccessStatus>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun abortJob(call: ApplicationCall, server: IngesterServer) {
    val jobId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed job ID.")

    transaction {
        val currentUser = call.currentUser()
        val job = Jobs.getById(jobId) ?: throw ErrorStatusException(404, "Job with ID $jobId could not be found.")

        /* Check if user's participant is the same as the one associated with the template. */
        currentUser.requireParticipant(job.template?.participantName, "You are not allowed to abort a job that has been created for another participant.")

        /* Check if job is still active. */
        if (job.status !in setOf(JobStatus.CREATED, JobStatus.HARVESTED, JobStatus.SCHEDULED, JobStatus.INGESTED, JobStatus.INTERRUPTED, JobStatus.RUNNING)) {
            throw ErrorStatusException(400, "Job with ID $jobId could not be aborted because it is already inactive.")
        }
        Jobs.update({ Jobs.id eq jobId }) { update ->
            update[status] = JobStatus.ABORTED
            update[modified] = Instant.now()
        }
    }

    /* Inform ingest server that job should be terminated.*/
    if (!server.terminateJob(jobId)) {
        call.respond(SuccessStatus("Successfully updated status of job $jobId."))
    } else {
        call.respond(SuccessStatus("Successfully terminated job $jobId."))
    }
}