package ch.pontius.kiar.database.job

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DbJobSource(entity: Entity) : XdEnumEntity(entity)  {
    companion object : XdEnumEntityType<DbJobStatus>() {
        val WATCHER by enumField { description = "WATCHER" }
        val WEB by enumField { description = "WEB" }
    }

    var description by xdRequiredStringProp(unique = true)
}