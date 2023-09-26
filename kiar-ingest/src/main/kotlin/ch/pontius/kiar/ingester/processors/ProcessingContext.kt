package ch.pontius.kiar.ingester.processors

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobLog
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.util.findById
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * A [ProcessingContext] that captures contextual information about a running job.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ProcessingContext(
    /** The ID of the job this [ProcessingContext] belongs to. */
    val jobId: String,

    /** The name of the participant this [ProcessingContext]. */
    val participant: String,

    /** */
    private val store: TransientEntityStore
) {
    /** Number of items processed in this [ProcessingContext]. */
    private val _processed = AtomicLong(0L)

    /** Number of items skipped in this [ProcessingContext]. */
    private val _skipped = AtomicLong(0L)

    /** Number of processing errors this [ProcessingContext]. */
    private val _error = AtomicLong(0L)

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

    /**
     * Increments the processed counter.
     */
    fun processed() {
        this._processed.incrementAndGet()
    }

    /**
     * Appends a [JobLog] entry to this [ProcessingContext],
     *
     * @param log The [JobLog] to append.
     */
    @Synchronized
    fun log(log: JobLog) {
        this.buffer.add(log)

        /* Errors are logged in statistics as well. */
        if (log.level in setOf(JobLogLevel.SEVERE, JobLogLevel.ERROR)) {
            this._error.incrementAndGet()
        }

        /* Validation errors are logged in statistics. */
        if (log.level == JobLogLevel.VALIDATION) {
            this._skipped.incrementAndGet()
        }

        if (buffer.size > 5000) {
            this.flushLogs()
        }
    }

    /**
     * Flushes all [JobLog]s to the database.
     */
    @Synchronized
    fun flushLogs() {
        this.store.transactional {
            val job = DbJob.findById(this@ProcessingContext.jobId)
            this.buffer.removeIf { log ->
                job.log.add(DbJobLog.new {
                    this.documentId = log.documentId
                    this.context = log.context.toDb()
                    this.level = log.level.toDb()
                    this.description = log.description
                })
                true
            }
        }
    }
}