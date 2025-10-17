package ch.pontius.kiar.migration.database.config.solr

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
/**
 * An enumeration of the types of [DbCollection] this KIAR tools instance knows.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbImageFormat(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbImageFormat>() {
        val JPEG by enumField { description = "JPEG"; suffix = ".jpg" }
        val PNG by enumField { description = "PNG"; suffix = ".png" }
    }

    /** The name of this [DbImageFormat]. */
    var description by xdRequiredStringProp(unique = true)

    /** The name of this [DbImageFormat]. */
    var suffix by xdRequiredStringProp(unique = true)

}