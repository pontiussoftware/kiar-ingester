package ch.pontius.kiar.ingester.watcher

import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import ch.pontius.kiar.api.model.job.JobSource
import ch.pontius.kiar.api.model.job.JobStatus
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.ingester.IngesterServer
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** The [KLogger] instance for [FileWatcher]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A [FileWatcher] is a [Runnable] that polls for a new file to be created. Once the file becomes availabe, it launches a new job.
 *
 * @author Ralph Gasser
 * @version 1.2.1
 */
class FileWatcher(private val server: IngesterServer, private val templateId: JobTemplateId, private val file: Path): Runnable {

    /** Flag indicating, that this [FileWatcher] has been cancelled. */
    @Volatile
    private var cancelled: Boolean = false

    /**
     * Runs and polls for changes to the watched directory until canceled.
     *
     * Whenever a file is detected, a new Job is scheduled.
     */
    override fun run() {
        /* Spinning loop polling for new file. */
        logger.info { "Added a file watcher for: ${this.file}" }
        while (!this.cancelled) {
            /* Now checks if file exists. Otherwise, we'll just continue polling... */
            if (!Files.exists(this.file)) {
                Thread.sleep(10_000)
                continue
            }

            /* Process the file by launching a Job. */
            logger.info { "New file detected: ${this.file}; scheduling job..." }
            try {
                /* Create new job. */
                val jobId = transaction {
                    val templateName = JobTemplates.select(JobTemplates.name).where { JobTemplates.id eq this@FileWatcher.templateId }.map { it[JobTemplates.name] }.first()
                    Jobs.insertAndGetId { insert ->
                        insert[name] = templateName + "-${System.currentTimeMillis()}"
                        insert[templateId] = this@FileWatcher.templateId
                        insert[src] = JobSource.WATCHER
                        insert[status] = JobStatus.HARVESTED
                        insert[createdBy] = "SYSTEM"
                    }
                }.value

                /* Move file to new location. */
                Files.move(this.file, this.file.parent.resolve("$jobId.job"), StandardCopyOption.ATOMIC_MOVE)

                /* Schedule job. */
                this.server.scheduleJob(jobId)
            } catch (e: Throwable) {
                logger.info { "Filed ${this.file} could not be processed due to exception: ${e.message}." }
            }
        }

        /* Spinning loop polling for new file. */
        logger.info { "Terminated file watcher for: ${this.file}" }
    }


    /**
     * Cancels this [FileWatcher].
     */
    fun cancel() {
        this.cancelled = true
    }
}