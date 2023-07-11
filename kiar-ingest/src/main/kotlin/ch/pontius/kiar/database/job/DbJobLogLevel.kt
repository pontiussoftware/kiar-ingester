package ch.pontius.kiar.database.job

import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.api.model.session.Role
import ch.pontius.kiar.database.institution.DbRole
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of [DbJobLog] log levels.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobLogLevel(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbJobLogLevel>() {
        val WARNING by enumField { description = "WARNING"; }
        val ERROR by enumField { description = "ERROR"; }
        val SEVERE by enumField { description = "SEVERE"; }
    }

    /** The name / description of this [DbJobLogLevel]. */
    var description by xdRequiredStringProp(unique = true)

    /**
     * A convenience method used to convert this [DbJobLogLevel] to a [JobLogLevel] instance.
     *
     * @return This [JobLogLevel].
     */
    fun toApi(): JobLogLevel = JobLogLevel.valueOf(this.description)
}