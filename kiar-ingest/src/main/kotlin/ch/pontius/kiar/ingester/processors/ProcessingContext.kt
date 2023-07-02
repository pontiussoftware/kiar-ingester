package ch.pontius.kiar.ingester.processors

import ch.pontius.kiar.api.model.job.JobLog
import java.util.*

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ProcessingContext(
    val name: String,

    @Volatile
    var processed: Long = 0L,

    @Volatile
    var skipped: Long = 0L,

    @Volatile
    var error: Long = 0L,

    val log: MutableList<JobLog> = Collections.synchronizedList(LinkedList())
)