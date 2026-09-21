package ch.pontius.kiar.tasks

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.jobs.JobLogs
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.Instant

/** The [KLogger] instance for [PurgeJobLogTask]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A periodic task that purges job log entries older than [Config.jobLogRetentionDays].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class PurgeJobLogTask(private val config: Config): SafeTimerTask() {
    override fun execute() {
        val threshold = Instant.now().minus(Duration.ofDays(this.config.jobLogRetentionDays.toLong()))
        val deleted = transaction {
            JobLogs.deleteWhere { JobLogs.created less threshold }
        }
        if (deleted > 0L) logger.info { "Purged $deleted job logs." }
    }
}
