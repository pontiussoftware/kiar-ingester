package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.database.job.DbJobLogContext

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class JobLogContext {
    METADATA,
    RESOURCE,
    SYSTEM;

    /**
     * Converts hit [Role] to a [DbJobLogLevel. Requires an active transaction.
     *
     * @return [DbJobLogContext]
     */
    fun toDb(): DbJobLogContext = when(this) {
        METADATA -> DbJobLogContext.METADATA
        RESOURCE -> DbJobLogContext.RESOURCE
        SYSTEM -> DbJobLogContext.SYSTEM
    }
}