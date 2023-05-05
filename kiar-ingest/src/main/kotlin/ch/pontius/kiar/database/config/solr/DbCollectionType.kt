package ch.pontius.kiar.database.config.solr

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of the types of [DbCollection] this KIAR tools instance knows.
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbCollectionType(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbCollectionType>() {
        val OBJECT by enumField { description = "OBJECT" }
        val PERSON by enumField { description = "PERSON" }
        val MUSEUM by enumField { description = "MUSEUM" }
    }

    /** The name of this [DbCollectionType]. */
    var description by xdRequiredStringProp(unique = true)
}