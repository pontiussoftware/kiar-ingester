package ch.pontius.kiar.ingester.processors

import ch.pontius.kiar.api.model.job.JobLog
import java.util.*

/**
 * A [ProcessingContext] that captures contextual information about a running job.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ProcessingContext(
    /** The ID of the job this [ProcessingContext] belongs to. */
    val jobId: String,

    /** The name of the participant this [ProcessingContext]. */
    val participant: String
) {
    /** Number of items processed in this [ProcessingContext]. */
    @Volatile
    var processed: Long = 0L

    /** Number of items skipped in this [ProcessingContext]. */
    @Volatile
    var skipped: Long = 0L

    /** Number of processing errors this [ProcessingContext]. */
    @Volatile
    var error: Long = 0L

    /** Append-only list of [JobLog] entries. */
    val log: MutableList<JobLog> = Collections.synchronizedList(LinkedList())
}