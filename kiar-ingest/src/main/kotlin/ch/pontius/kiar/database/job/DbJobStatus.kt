package ch.pontius.kiar.database.job

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of potential status for a [DbJob].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobStatus(entity: Entity) : XdEnumEntity(entity)  {
    companion object : XdEnumEntityType<DbJobStatus>() {
        val CREATED by enumField { description = "CREATED" }
        val ABORTED by enumField { description = "ABORTED" }
        val HARVESTED by enumField { description = "HARVESTED" }
        val RUNNING by enumField { description = "RUNNING" }
        val INGESTED by enumField { description = "INGESTED" }
    }

    var description by xdRequiredStringProp(unique = true)
}