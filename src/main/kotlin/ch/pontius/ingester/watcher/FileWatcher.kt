package ch.pontius.ingester.watcher

import ch.pontius.ingester.IngesterServer
import ch.pontius.ingester.config.JobConfig
import org.apache.logging.log4j.LogManager
import java.io.Closeable
import java.nio.file.*
import kotlin.system.measureTimeMillis


/**
 * A [FileWatcher] is a [Runnable] that polls for a new file to be created. Once the file becomes availabe, it launces a new job.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class FileWatcher(private val server: IngesterServer, jobConfig: JobConfig): Runnable, Closeable {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The [WatchService] used to watch for new files. */
    private val service: WatchService = FileSystems.getDefault().newWatchService()

    /** The path to the file this [FileWatcher] is waiting for. */
    private val file: Path = jobConfig.file

    /** The parent folder containing the file (which is the entity being observed). */
    private val folder = this.file.parent

    /** The name of the file we are waiting for.*/
    private val fileName = this.file.fileName

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
        /* Generate WatchKey. */
        val key = try {
            this.folder.register(this.service, StandardWatchEventKinds.ENTRY_CREATE)
        } catch (e: Throwable) {
            LOGGER.error("Failed to generate a watch key for ${this.file}: ${e.message}")
            return
        }

        /* Spinning loop polling for events. */
        LOGGER.info("Added a file watcher for ${this.file}.")
        while (!this.canceled) {
            val events = key.pollEvents()
            if (events.isNotEmpty()) {
                for (event in events) {
                    if (event.context() == this.fileName) {
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
                                Files.move(this.file, this.file.parent.resolve("${this.fileName}~${System.currentTimeMillis()}"))
                            }
                        } catch (e: Throwable) {
                            LOGGER.error("Cleanup of ${this.file} failed due to exception: ${e.message}.")
                        }
                        break
                    }
                }
                key.reset()
            }
            Thread.sleep(1000)
        }

        /* Cancel the WatchKey. */
        key.cancel()
    }

    /**
     * Closes this [FileWatcher].
     */
    override fun close() {
        if (!this.canceled) {
            this.canceled = true
            this.service.close()
        }
    }
}