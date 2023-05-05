package ch.pontius.kiar.database.config.jobs

import ch.pontius.kiar.database.config.mapping.DbEntityMapping
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration that encodes the different types of [DbJobTemplate]s supported by the KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobType(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbJobType>() {
        val XML by DbJobType.enumField { description = "XML"; suffix = "xml" }
        val JSON by DbJobType.enumField { description = "JSON"; suffix = "json" }
        val KIAR by DbJobType.enumField { description = "KIAR"; suffix = "kiar" }
    }

    /** The name of this [DbJobType]. */
    var description by xdRequiredStringProp(unique = true)

    /** The suffix of input files of this [DbJobType]. */
    var suffix by xdRequiredStringProp(unique = true)
}