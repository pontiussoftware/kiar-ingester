package ch.pontius.ingester.watcher

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.file.*
import kotlin.system.measureTimeMillis


/**
 * A [FileWatcher] is a [Runnable] that polls for a new file to be created and the hands that file to a [FileSource]
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class FileWatcher(val file: Path, val jobName: String): Runnable, Closeable {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FileWatcher::class.java)
    }

    /** The [WatchService] used to watch for new files. */
    private val service: WatchService = FileSystems.getDefault().newWatchService()


    /** */
    private val folder = this.file.parent

    /** */
    private val fileName = this.file.fileName.toString()

    /** Flag indicating whether this [FileWatcher] has been closed. */
    @Volatile
    private var canceled: Boolean = false

    init {
        val folder = this.file.parent
        folder.register(this.service, StandardWatchEventKinds.ENTRY_CREATE)
    }

    /**
     * Runs and polls for changes to the watched directory until canceled.
     *
     * Whenever a file is detected, the file is forwarded to the [FileSource]
     */
    override fun run() {
        while (!this.canceled) {
            val next = this.service.poll()
            if (next != null) {
                try {
                    for (event in next.pollEvents()) {
                        if (event.context() == this.fileName) {
                            var error = false
                            LOGGER.info("New file detected: ${this.file}; starting processing...")
                            try {
                                val duration = measureTimeMillis {
                                    //this.processor.process(this.folder.resolve(this.forFile))
                                }
                                LOGGER.info("Processing of ${this.file} completed in $duration ms.")

                            } catch (e: Throwable) {
                                LOGGER.error("Processing of ${this.file} failed due to exception: ${e.message}.")
                                error = true
                            }
                        }
                    }
                } finally {
                    next.reset()
                }
            }
            Thread.sleep(500)
        }
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