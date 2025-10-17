package ch.pontius.kiar.tasks

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.jobs.JobLogs
import ch.pontius.kiar.ingester.IngesterServer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * A simple [TimerTask] used to schedule the purging of old [JobLogs] entries.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class PurgeJobLogTask(private val config: Config): TimerTask() {
    companion object {
        /** The [Logger] used by this [IngesterServer]. */
        private val LOGGER: Logger = LogManager.getLogger()
    }

    override fun run() {
        val deleted = transaction {
            val threshold = LocalDateTime.now().minusDays(this@PurgeJobLogTask.config.jobLogRetentionDays.toLong()).toInstant(ZoneOffset.UTC)
            JobLogs.deleteWhere { JobLogs.created less threshold }
        }
        if (deleted > 0L) LOGGER.info("Purged $deleted job logs.")
    }
}