package ch.pontius.kiar.database.config.mapping

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbParser(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbParser>() {
        val XML by DbParser.enumField { description = "XML" }
        val JSON by DbParser.enumField { description = "JSON" }
    }

    /** The name of this [DbParser]. */
    var description by xdRequiredStringProp(unique = true)
}
