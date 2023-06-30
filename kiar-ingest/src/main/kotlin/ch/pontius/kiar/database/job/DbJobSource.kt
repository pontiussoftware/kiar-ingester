package ch.pontius.kiar.database.job

import ch.pontius.kiar.api.model.job.JobSource
import ch.pontius.kiar.api.model.session.Role
import ch.pontius.kiar.database.institution.DbRole
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of the potential source of a [DbJob].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobSource(entity: Entity) : XdEnumEntity(entity)  {
    companion object : XdEnumEntityType<DbJobSource>() {
        val WATCHER by enumField { description = "WATCHER" }
        val WEB by enumField { description = "WEB" }
    }

    var description by xdRequiredStringProp(unique = true)

    /**
     * A convenience method used to convert this [DbJobSource] to a [JobSource] instance.
     *
     * Requires an ongoing transaction.
     *
     * @return This [JobSource].
     */
    fun toApi(): JobSource = JobSource.valueOf(this.description)
}