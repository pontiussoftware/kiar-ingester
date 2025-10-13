package ch.pontius.kiar.ingester.processors

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobLog
import kotlinx.dnq.util.findById
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * A [ProcessingContext] that captures contextual information about a running job.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class ProcessingContext(
    /** The ID of the job this [ProcessingContext] belongs to. */
    val jobId: String,

    /** The name of the participant this [ProcessingContext]. */
    val participant: String,
) {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** Number of items processed in this [ProcessingContext]. */
    private val _processed = AtomicLong(0L)

    /** Number of items skipped in this [ProcessingContext]. */
    private val _skipped = AtomicLong(0L)

    /** Number of processing errors this [ProcessingContext]. */
    private val _error = AtomicLong(0L)

    /** Number of processing errors this [ProcessingContext]. */
    private val _aborted = AtomicBoolean(false)

    /** Append-only list of [JobLog] entries. */
    private val buffer: MutableList<JobLog> = Collections.synchronizedList(LinkedList())

    /** Number of items processed in this [ProcessingContext]. */
    val processed: Long
        get() = this._processed.get()

    /** Number of items skipped in this [ProcessingContext]. */
    val skipped: Long
        get() = this._skipped.get()

    /** Number of processing errors in this [ProcessingContext]. */
    val error: Long
        get() = this._error.get()

    /** Flag indicating, that this job has been aborted. */
    val aborted: Boolean
        get() = this._aborted.get()

    /**
     * Increments the processed counter.
     */
    fun processed() {
        this._processed.incrementAndGet()
    }

    /**
     * Aborts the job associated with this [ProcessingContext].
     */
    fun abort() = this._aborted.set(true)

    /**
     * Appends a [JobLog] entry to this [ProcessingContext],
     *
     * @param log The [JobLog] to append.
     */
    fun log(log: JobLog) {
        this.buffer.add(log)

        /* Process event. */
        when(log.level) {
            JobLogLevel.WARNING -> LOGGER.info(log.description)
            JobLogLevel.VALIDATION -> {
                LOGGER.warn(log.description)
                this._skipped.incrementAndGet()
            }
            JobLogLevel.ERROR,
            JobLogLevel.SEVERE -> {
                this._error.incrementAndGet()
                LOGGER.error(log.description)
            }
        }
    }

    /** Returns the size of the current log. */
    fun logSize(): Int = this.buffer.size

    /**
     * Flushes all [JobLog]s to the database.
     *
     * Requires an ongoing transaction.
     */
    fun flushLogs() {
        val job = DbJob.findById(this@ProcessingContext.jobId)
        this.buffer.removeIf { log ->
            job.log.add(DbJobLog.new {
                this.documentId = log.documentId.toString()
                this.context = log.context.toDb()
                this.level = log.level.toDb()
                this.description = log.description
            })
            true
        }
    }
}