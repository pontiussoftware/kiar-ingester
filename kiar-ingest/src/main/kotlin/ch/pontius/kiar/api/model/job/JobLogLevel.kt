package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.api.model.session.Role
import ch.pontius.kiar.database.job.DbJobLogLevel

/**
 * The [JobLogLevel] of a [JobLog] entry.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class JobLogLevel {
    WARNING,
    ERROR,
    VALIDATION,
    SEVERE;

    /**
     * Converts hit [Role] to a [DbJobLogLevel. Requires an active transaction.
     *
     * @return [DbJobLogLevel]
     */
    fun toDb(): DbJobLogLevel = when(this) {
        WARNING -> DbJobLogLevel.WARNING
        ERROR -> DbJobLogLevel.ERROR
        VALIDATION -> DbJobLogLevel.VALIDATION
        SEVERE -> DbJobLogLevel.SEVERE
    }
}