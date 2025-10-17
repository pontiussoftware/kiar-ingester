package ch.pontius.kiar.migration.database.config.mapping

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
class DbParser(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbParser>() {
        val UUID by enumField { description = "UUID" }
        val STRING by enumField { description = "STRING" }
        val MULTISTRING by enumField { description = "MULTISTRING" }
        val DATE by enumField { description = "DATE" }
        val INTEGER by enumField { description = "INTEGER" }
        val DOUBLE by enumField { description = "DOUBLE" }

        /* Struct parsers. */
        val COORD_WGS84 by enumField { description = "COORD_WGS84" }
        val COORD_LV95 by enumField { description = "COORD_LV95" }

        /* Image parsers. */
        val IMAGE_FILE by enumField { description = "IMAGE_FILE" }
        val IMAGE_MPLUS by enumField { description = "IMAGE_MPLUS" }
    }

    /** The name of this [DbParser]. */
    var description by xdRequiredStringProp(unique = true)
}
