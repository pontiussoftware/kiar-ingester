package ch.pontius.kiar.database.config.mapping

import ch.pontius.kiar.api.model.config.mappings.ValueParser
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
        val UUID by DbParser.enumField { description = "UUID" }
        val STRING by DbParser.enumField { description = "STRING" }
        val MULTISTRING by DbParser.enumField { description = "MULTISTRING" }
        val DATE by DbParser.enumField { description = "DATE" }
        val INTEGER by DbParser.enumField { description = "INTEGER" }
        val DOUBLE by DbParser.enumField { description = "DOUBLE" }
        val IMAGE by DbParser.enumField { description = "IMAGE" }
    }

    /** The name of this [DbParser]. */
    var description by xdRequiredStringProp(unique = true)

    /**
     * A convenience method used to convert this [DbEntityMapping] to a [ValueParser] instance.
     *
     * @return This [ValueParser].
     */
    fun toApi(): ValueParser = ValueParser.valueOf(this.description)
}
