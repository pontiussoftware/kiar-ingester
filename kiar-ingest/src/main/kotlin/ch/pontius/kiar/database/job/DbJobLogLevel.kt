package ch.pontius.kiar.database.job

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
}