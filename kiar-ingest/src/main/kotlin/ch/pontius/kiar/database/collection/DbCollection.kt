package ch.pontius.kiar.database.collection

import ch.pontius.kiar.api.model.collection.Collection
import ch.pontius.kiar.database.institution.DbInstitution
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A [DbCollection] as managed by the KIAR Uploader Tool.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbCollection(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbCollection>()

    /** The name held by this [DbCollection]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The display name held by this [DbCollection]. */
    var displayName by xdRequiredStringProp(trimmed = true)

    /** The [DbInstitution] this [DbInstitution] belongs to. */
    var participant by xdLink1(DbInstitution)

    /** A brief description for this [DbCollection]. */
    var description by xdRequiredStringProp(trimmed = true)

    /** The images associated with this [DbCollection]. */
    var images by xdSetProp<DbCollection, String>()

    /**
     * Convenience method to convert this [DbCollection] to a [Collection].
     *
     * Requires an ongoing transaction.
     *
     * @return [Collection]
     */
    fun toApi() = Collection(
        id = this.xdId,
        name = this.name,
        displayName = this.displayName,
        description = this.description,
        images = this.images.toList(),
        institution = this.participant.toApi()
    )
}