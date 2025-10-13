package ch.pontius.kiar.migration.database.job

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdBooleanProp
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of potential status for a [DbJob].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobStatus(entity: Entity) : XdEnumEntity(entity)  {
    companion object : XdEnumEntityType<DbJobStatus>() {
        val CREATED by enumField { description = "CREATED"; active = true }
        val HARVESTED by enumField { description = "HARVESTED"; active = true }
        val RUNNING by enumField { description = "RUNNING"; active = true }
        val INTERRUPTED by enumField { description = "INTERRUPTED"; active = true }
        val SCHEDULED by enumField { description = "SCHEDULED"; active = true }
        val FAILED by enumField { description = "FAILED"; active = true }
        val ABORTED by enumField { description = "ABORTED"; active = false }
        val INGESTED by enumField { description = "INGESTED"; active = false }
    }

    /** The name / description of this [DbJobStatus]. */
    var description by xdRequiredStringProp(unique = true)

    /** Flag indicating, whether this [DbJobStatus] is considered active. */
    var active by xdBooleanProp()
}