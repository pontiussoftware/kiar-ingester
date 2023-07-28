package ch.pontius.kiar.database.config.transformers

import ch.pontius.kiar.api.model.config.transformers.TransformerType
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
        val DISPLAY by DbTransformerType.enumField { description = "DISPLAY" }
        val SYSTEM by DbTransformerType.enumField { description = "SYSTEM" }
    }

    /** The name of this [DbTransformerType]. */
    var description by xdRequiredStringProp(unique = true)

    /**
     * Convenience method to convert this [DbTransformerType] to a [TransformerType].
     *
     * Requires an ongoing transaction.
     *
     * @return [TransformerType]
     */
    fun toApi() = TransformerType.valueOf(this.description)
}