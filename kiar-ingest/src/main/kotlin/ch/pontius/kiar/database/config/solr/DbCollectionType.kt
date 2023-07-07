package ch.pontius.kiar.database.config.solr

import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.database.config.jobs.DbJobType
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of the types of [DbCollection] this KIAR tools instance knows.
 *
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

    /**
     * Convenience method to convert this [DbCollectionType] to a [CollectionType].
     *
     * Requires an ongoing transaction.
     *
     * @return [CollectionType]
     */
    fun toApi() = CollectionType.valueOf(this.description)
}