package ch.pontius.kiar.migration.database.collection

import ch.pontius.kiar.database.collections.Collections
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.migration.database.institution.DbInstitution
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * A [DbObjectCollection] as managed by the KIAR Uploader Tool.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbObjectCollection(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbObjectCollection>() {
        fun migrate() {
            all().asSequence().forEach { dbCollection ->
                Collections.insert {
                    it[institutionId] = Institutions.idByName(dbCollection.institution.name) ?: throw IllegalStateException("Could not find institution with name '${dbCollection.institution.name}'.")
                    it[name] = dbCollection.name
                    it[displayName] = dbCollection.displayName
                    it[publish] = dbCollection.publish
                    it[description] = dbCollection.description
                    it[filters] = dbCollection.filters.toTypedArray()
                    it[images] = dbCollection.images.toTypedArray()
                }
            }
        }
    }

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
}