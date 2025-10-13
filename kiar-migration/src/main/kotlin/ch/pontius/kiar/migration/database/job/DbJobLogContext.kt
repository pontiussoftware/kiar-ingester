package ch.pontius.kiar.migration.database.job

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of [DbJobLog] context identifiers.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobLogContext(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbJobLogContext>() {
        val METADATA by enumField { description = "METADATA"; }
        val RESOURCE by enumField { description = "RESOURCE"; }
        val SYSTEM by enumField { description = "SYSTEM"; }
    }

    /** The name / description of this [DbJobLogContext]. */
    var description by xdRequiredStringProp(unique = true)
}