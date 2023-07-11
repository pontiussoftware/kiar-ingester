package ch.pontius.kiar.ingester.watcher

import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobSource
import ch.pontius.kiar.database.job.DbJobStatus
import ch.pontius.kiar.ingester.IngesterServer
import kotlinx.dnq.util.findById
import org.apache.logging.log4j.LogManager
import org.joda.time.DateTime
import java.nio.file.*


/**
 * A [FileWatcher] is a [Runnable] that polls for a new file to be created. Once the file becomes availabe, it launches a new job.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class FileWatcher(private val server: IngesterServer, private val templateId: String, private val file: Path): Runnable {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** Flag indicating, that this [FileWatcher] has been cancelled. */
    @Volatile
    private var cancelled: Boolean = false

    /**
     * Runs and polls for changes to the watched directory until canceled. Whenever a file is detected,
     * the file is forwarded to the [FileSource].
     */
    override fun run() {
        /* Spinning loop polling for new file. */
        LOGGER.info("Added a file watcher for: ${this.file}")
        while (!this.cancelled) {
            /* Now checks if file exists. Otherwise, we'll just continue polling... */
            if (!Files.exists(this.file)) {
                Thread.sleep(10_000)
                continue
            }

            /* Process the file by launching a Job. */
            LOGGER.info("New file detected: ${this.file}; scheduling job...")
            try {
                /* Create new job. */
                val jobId = this@FileWatcher.server.store.transactional {
                    val template = DbJobTemplate.findById(this@FileWatcher.templateId)
                    DbJob.new {
                        this.name = template.name + "-${System.currentTimeMillis()}"
                        this.template = template
                        this.source = DbJobSource.WATCHER
                        this.status = DbJobStatus.CREATED
                        this.createdAt = DateTime.now()
                        this.createdByName = "SYSTEM"
                    }.xdId
                }

                /* Move file to new location. */
                Files.move(this.file, this.file.parent.resolve(jobId), StandardCopyOption.ATOMIC_MOVE)

                /* Schedule job. */
                this.server.scheduleJob(jobId)
            } catch (e: Throwable) {
                LOGGER.error("Filed ${this.file} could not be processed due to exception: ${e.message}.")
                Files.move(this.file, this.file.parent.resolve(this.file.fileName.toString() + "~err"), StandardCopyOption.ATOMIC_MOVE)
            }
        }

        /* Spinning loop polling for new file. */
        LOGGER.info("Terminated file watcher for: ${this.file}")
    }


    /**
     * Cancels this [FileWatcher].
     */
    fun cancel() {
        this.cancelled = true
    }
}