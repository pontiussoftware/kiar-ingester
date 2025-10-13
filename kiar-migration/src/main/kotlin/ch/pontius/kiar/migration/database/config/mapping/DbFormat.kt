package ch.pontius.kiar.migration.database.config.mapping

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of the types of [DbEntityMapping].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbFormat(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbFormat>() {
        val XML by enumField { description = "XML" }
        val JSON by enumField { description = "JSON" }
        val EXCEL by enumField { description = "EXCEL" }
    }

    /** The name of this [DbEntityMapping]. */
    var description by xdRequiredStringProp(unique = true)
}