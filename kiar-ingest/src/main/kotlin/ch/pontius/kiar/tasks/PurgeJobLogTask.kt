package ch.pontius.kiar.tasks

import ch.pontius.kiar.database.job.DbJobLog
import ch.pontius.kiar.ingester.IngesterServer
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.joda.time.DateTime
import java.util.*

/**
 * A simple [TimerTask] used to schedule the purging of old [DbJobLog] entries.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class PurgeJobLogTask(private val store: TransientEntityStore, private val retentionTime: Int): TimerTask() {
    companion object {
        /** The [Logger] used by this [IngesterServer]. */
        private val LOGGER: Logger = LogManager.getLogger()
    }

    override fun run() {
        var deleted = 0L
        this.store.transactional { transaction ->
            val threshold = DateTime.now().minusDays(this.retentionTime)
            DbJobLog.filter { it.job.createdAt le threshold }.asSequence().forEach {
                it.delete()
                deleted++
                if (deleted % 100_000L == 0L) {
                    transaction.flush()
                    LOGGER.info("Purged $deleted job logs.")
                }
            }
        }
        if (deleted > 0L) {
            LOGGER.info("Purged $deleted job logs.")
        }
    }
}