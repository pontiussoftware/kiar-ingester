package ch.pontius.kiar.migration.database.job

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
}