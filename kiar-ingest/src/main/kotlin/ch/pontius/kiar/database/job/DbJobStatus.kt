package ch.pontius.kiar.database.job

import ch.pontius.kiar.api.model.job.JobStatus
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
        val SCHEDULED by enumField { description = "SCHEDULED"; active = true }
        val ABORTED by enumField { description = "ABORTED"; active = false }
        val INGESTED by enumField { description = "INGESTED"; active = false }
        val FAILED by enumField { description = "FAILED"; active = false }
    }

    /** The name / description of this [DbJobStatus]. */
    var description by xdRequiredStringProp(unique = true)

    /** Flag indicating, whether this [DbJobStatus] is considered active. */
    var active by xdBooleanProp()

    /**
     * A convenience method used to convert this [DbJobStatus] to a [JobStatus] instance.
     *
     * Requires an ongoing transaction.
     *
     * @return This [JobStatus].
     */
    fun toApi(): JobStatus = JobStatus.valueOf(this.description)
}