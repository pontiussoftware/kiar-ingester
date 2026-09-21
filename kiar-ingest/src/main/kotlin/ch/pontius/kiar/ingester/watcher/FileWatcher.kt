package ch.pontius.kiar.ingester.watcher

import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import ch.pontius.kiar.api.model.job.JobSource
import ch.pontius.kiar.api.model.job.JobStatus
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.ingester.IngesterServer
import ch.pontius.kiar.ingester.watcher.FileWatcher.Companion.SETTLE_INTERVAL_MS
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
 * A [FileWatcher] polls for a file at a fixed location and, once it appears and has stopped changing, claims it,
 * creates a [Jobs] entry for it and schedules the job.
 *
 * The file is claimed (moved to a staging name) before the job row is inserted, so that a file that cannot be moved
 * (still being written, locked, permissions) never leaves orphaned job rows behind. Every failure backs off before
 * the next attempt.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class FileWatcher(private val server: IngesterServer, private val templateId: JobTemplateId, private val file: Path): Runnable {

    companion object {
        /** Interval between polls for the watched file. */
        private const val POLL_INTERVAL_MS = 10_000L

        /** Time a file must remain unchanged (size and modification time) before it is considered complete. */
        private const val SETTLE_INTERVAL_MS = 5_000L

        /** Back-off after a failed attempt to claim or schedule a file. */
        private const val RETRY_INTERVAL_MS = 30_000L
    }

    /** Flag indicating whether this [FileWatcher] has been cancelled. */
    @Volatile
    private var cancelled: Boolean = false

    override fun run() {
        logger.info { "Added a file watcher for: ${this.file}" }
        try {
            while (!this.cancelled) {
                /* Poll until the file exists. */
                if (!Files.exists(this.file)) {
                    Thread.sleep(POLL_INTERVAL_MS)
                    continue
                }

                /* Wait until the file has stopped changing; a file that is still being copied must not be picked up. */
                if (!this.isSettled()) {
                    continue
                }

                /* Claim the file, create the job and schedule it. */
                logger.info { "New file detected: ${this.file}; scheduling job..." }
                val jobId = try {
                    this.claimAndCreateJob()
                } catch (e: Throwable) {
                    logger.warn(e) { "File ${this.file} could not be claimed for processing; retrying in ${RETRY_INTERVAL_MS / 1000}s." }
                    Thread.sleep(RETRY_INTERVAL_MS)
                    continue
                }

                try {
                    this.server.scheduleJob(jobId)
                } catch (e: Throwable) {
                    /* The job's own status has been recorded by the server; just keep watching. */
                    logger.warn(e) { "Job $jobId created from ${this.file} could not be scheduled." }
                }
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        logger.info { "Terminated file watcher for: ${this.file}" }
    }

    /**
     * Returns true if the watched file's size and modification time did not change over [SETTLE_INTERVAL_MS].
     */
    private fun isSettled(): Boolean {
        val before = this.fingerprint() ?: return false
        Thread.sleep(SETTLE_INTERVAL_MS)
        val after = this.fingerprint() ?: return false
        if (before != after) {
            logger.debug { "File ${this.file} is still changing; waiting." }
            return false
        }
        return true
    }

    /**
     * Returns the (size, last modified) fingerprint of the watched file or null if it cannot be read.
     */
    private fun fingerprint(): Pair<Long, Long>? = try {
        Files.size(this.file) to Files.getLastModifiedTime(this.file).toMillis()
    } catch (_: Throwable) {
        null
    }

    /**
     * Moves the watched file to a staging name, creates the [Jobs] entry and renames the staged file to its final name.
     *
     * If the job entry cannot be created, the staged file is moved back so that the next attempt can pick it up again.
     *
     * @return The ID of the created job.
     */
    private fun claimAndCreateJob(): Int {
        val staged = this.file.resolveSibling("${this.file.fileName}.processing")
        Files.move(this.file, staged, StandardCopyOption.ATOMIC_MOVE)
        val jobId = try {
            transaction {
                val templateName = JobTemplates.select(JobTemplates.name).where { JobTemplates.id eq this@FileWatcher.templateId }.map { it[JobTemplates.name] }.firstOrNull()
                    ?: throw IllegalStateException("Job template ${this@FileWatcher.templateId} no longer exists.")
                Jobs.insertAndGetId { insert ->
                    insert[name] = templateName + "-${System.currentTimeMillis()}"
                    insert[templateId] = this@FileWatcher.templateId
                    insert[src] = JobSource.WATCHER
                    insert[status] = JobStatus.HARVESTED
                    insert[createdBy] = "SYSTEM"
                }.value
            }
        } catch (e: Throwable) {
            try {
                Files.move(staged, this.file, StandardCopyOption.ATOMIC_MOVE)
            } catch (moveBack: Throwable) {
                logger.error(moveBack) { "Failed to restore ${this.file} after job creation failed; the file remains at $staged." }
            }
            throw e
        }
        Files.move(staged, this.file.resolveSibling("$jobId.job"), StandardCopyOption.ATOMIC_MOVE)
        return jobId
    }

    /**
     * Cancels this [FileWatcher]; the polling loop terminates after the current iteration.
     */
    fun cancel() {
        this.cancelled = true
    }
}
