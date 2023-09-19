package ch.pontius.kiar.api.model.config.templates

import ch.pontius.kiar.database.config.jobs.DbJobType

/**
 * Enumeration of the available [JobType].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
enum class JobType {
    XML, JSON, EXCEL, KIAR;

    /**
     * Convenience method to convert this [JobType] to a [DbJobType]. Requires an ongoing transaction!
     *
     * @return [DbJobType]
     */
    fun toDb(): DbJobType = when(this) {
        XML -> DbJobType.XML
        JSON ->  DbJobType.JSON
        EXCEL -> DbJobType.EXCEL
        KIAR ->  DbJobType.KIAR
    }
}