package ch.pontius.kiar.database.collection

import ch.pontius.kiar.api.model.collection.ObjectCollection
import ch.pontius.kiar.database.institution.DbInstitution
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A [DbObjectCollection] as managed by the KIAR Uploader Tool.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbObjectCollection(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbObjectCollection>()

    /** The name held by this [DbObjectCollection]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The display name held by this [DbObjectCollection]. */
    var displayName by xdRequiredStringProp(trimmed = true)

    /** Flag indicating whether this [DbObjectCollection]'s metadata should be published. */
    var publish by xdBooleanProp()

    /** The [DbInstitution] this [DbInstitution] belongs to. */
    var institution by xdLink1(DbInstitution)

    /** A brief description for this [DbObjectCollection]. */
    var description by xdRequiredStringProp(trimmed = true)

    /** The filters associated with this [DbObjectCollection]. */
    var filters by xdSetProp<DbObjectCollection, String>()

    /** The images associated with this [DbObjectCollection]. */
    var images by xdSetProp<DbObjectCollection, String>()

    /**
     * Convenience method to convert this [DbObjectCollection] to a [ObjectCollection].
     *
     * Requires an ongoing transaction.
     *
     * @return [ObjectCollection]
     */
    fun toApi() = ObjectCollection(
        id = this.xdId,
        name = this.name,
        displayName = this.displayName,
        publish = this.publish,
        institutionName = this.institution.name,
        description = this.description,
        filters = this.filters.toList(),
        images = this.images.toList()
    )
}