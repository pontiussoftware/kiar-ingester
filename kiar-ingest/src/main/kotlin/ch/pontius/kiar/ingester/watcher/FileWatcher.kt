package ch.pontius.kiar.ingester.watcher

import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.ingester.IngesterServer
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.isNotEmpty
import kotlinx.dnq.query.size
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.*
import kotlin.system.measureTimeMillis


/**
 * A [FileWatcher] is a [Runnable] that polls for a new file to be created. Once the file becomes availabe, it launches a new job.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class FileWatcher(private val server: IngesterServer, private val templateName: String, private val file: Path): Runnable {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** Flag indicating whether file should be deleted once processing has concluded. */
    private val delete: Boolean = false

    /**
     * Runs and polls for changes to the watched directory until canceled. Whenever a file is detected,
     * the file is forwarded to the [FileSource].
     */
    override fun run() {
        /* Spinning loop polling for new file. */
        LOGGER.info("Added a file watcher for: ${this.file}")
        while (true) {
            /* Checks if template still exists; terminates watcher if template has been removed. */
            if (!this.templateStillValid()) {
                break
            }

            /* Now checks if file exists. Otherwise we'll just continue polling... */
            if (!Files.exists(this.file)) {
                Thread.sleep(10_000)
                continue
            }

            /* Process the file by launching a Job. */
            LOGGER.info("New file detected: ${this.file}; scheduling job...")
            try {
                val duration = measureTimeMillis {
                    this.server.execute(this.templateName)
                }
                LOGGER.info("Processing of ${this.file} completed in $duration ms.")
            } catch (e: Throwable) {
                LOGGER.error("Processing of ${this.file} failed due to exception: ${e.message}.")
            }

            /* Perform cleanup. */
            try {
                if (this.delete) {
                    Files.delete(this.file)
                } else {
                    Files.move(this.file, this.file.parent.resolve("${this.file.fileName}~${System.currentTimeMillis()}"))
                }
            } catch (e: IOException) {
                LOGGER.error("Cleanup of ${this.file} failed due to IO exception: ${e.message}. Polling is aborted...")
                break
            }
        }

        /* Spinning loop polling for new file. */
        LOGGER.info("Terminated file watcher for: ${this.file}")
    }

    /**
     * Checks if the [DbJobTemplate] backing this [FileWatcher] is still valid (i.e., has not been deleted).
     *
     * @return True, if [DbJobTemplate] still exists.
     */
    private fun templateStillValid(): Boolean = this.server.store.transactional(true) {
        DbJobTemplate.filter { (it.name eq this@FileWatcher.templateName) and (it.startAutomatically eq true) }.isNotEmpty
    }
}