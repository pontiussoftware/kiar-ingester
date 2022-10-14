package ch.pontius.ingester.watcher

import ch.pontius.ingester.IngesterServer
import ch.pontius.ingester.config.JobConfig
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.*
import kotlin.system.measureTimeMillis


/**
 * A [FileWatcher] is a [Runnable] that polls for a new file to be created. Once the file becomes availabe, it launces a new job.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class FileWatcher(private val server: IngesterServer, jobConfig: JobConfig): Runnable {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The path to the file this [FileWatcher] is waiting for. */
    private val file: Path = jobConfig.file

    /** The name of the job that should be executed once the file is created. */
    private val jobName = jobConfig.name

    /** Flag indicating whether file should be deleted once processing has concluded. */
    private val delete: Boolean = false

    /** Flag indicating whether this [FileWatcher] has been closed. */
    @Volatile
    private var canceled: Boolean = false

    /**
     * Runs and polls for changes to the watched directory until canceled. Whenever a file is detected,
     * the file is forwarded to the [FileSource].
     */
    override fun run() {
        /* Spinning loop polling for new file. */
        LOGGER.info("Added a file watcher for ${this.file}.")
        while (!this.canceled) {
            try {
                if (Files.exists(this.file)) {
                    LOGGER.info("New file detected: ${this.file}; scheduling job...")

                    /* Process the file by launching a Job. */
                    try {
                        val duration = measureTimeMillis {
                            this.server.execute(this.jobName)
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
                Thread.sleep(1000) /* Poll is done every 10s. */
            } catch (e: IOException) {
                LOGGER.error("Polling for file failed due to IO exception: ${e.message}. Polling is aborted...")
                break
            }
        }
    }

    /**
     * Closes this [FileWatcher].
     */
    @Synchronized
    fun cancel() {
        if (!this.canceled) {
            this.canceled = true
        }
    }
}