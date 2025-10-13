package ch.pontius.kiar.migration.database.config.transformers

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of the types of data transformers.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class DbTransformerType(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbTransformerType>() {
        val DISPLAY by enumField { description = "DISPLAY" }
        val SYSTEM by enumField { description = "SYSTEM" }
        val RIGHTS by enumField { description = "RIGHTS" }
        val UUID by enumField { description = "UUID" }
    }

    /** The name of this [DbTransformerType]. */
    var description by xdRequiredStringProp(unique = true)
}