package ch.pontius.kiar.tasks

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.jobs.JobLogs
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/** The [KLogger] instance for [PurgeJobLogTask]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A simple [TimerTask] used to schedule the purging of old [JobLogs] entries.
 *
 * @author Ralph Gasser
 * @version 1.1.1
 */
class PurgeJobLogTask(private val config: Config): TimerTask() {
    override fun run() {
        val deleted = transaction {
            val threshold = LocalDateTime.now().minusDays(this@PurgeJobLogTask.config.jobLogRetentionDays.toLong()).toInstant(ZoneOffset.UTC)
            JobLogs.deleteWhere { JobLogs.created less threshold }
        }
        if (deleted > 0L) logger.info { "Purged $deleted job logs." }
    }
}