package ch.pontius.kiar.database.config.transformers

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of the types of data transformers.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbTransformerType(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbTransformerType>() {
        val IMAGE by DbTransformerType.enumField { description = "IMAGE" }
        val SYSTEM by DbTransformerType.enumField { description = "SYSTEM" }
    }

    /** The name of this [DbTransformerType]. */
    var description by xdRequiredStringProp(unique = true)
}