package ch.pontius.kiar.migration.database.config.mapping

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A storable entity mapping configuration as used by the KIAR Tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbEntityMapping(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbEntityMapping>()

    /** The name held by this [DbEntityMapping]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** A brief description of this [DbEntityMapping].*/
    var description by xdStringProp(trimmed = true)

    /** The [DbFormat] of this [DbEntityMapping]. */
    var type by xdLink1(DbFormat)

    /** The date and time this [DbEntityMapping] was created. */
    var createdAt by xdDateTimeProp()

    /** The date and time this [DbEntityMapping] was last changed. */
    var changedAt by xdDateTimeProp()

    /** The [DbAttributeMapping]s that belong to this [DbEntityMapping]. */
    val attributes by xdChildren0_N(DbAttributeMapping::mapping)
}