package ch.pontius.kiar.database.config.mapping

import ch.pontius.kiar.database.config.jobs.DbJobTemplate
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
    val type by xdLink1(DbFormat)

    /** The [DbJobTemplate] this [DbEntityMapping] belongs to. */
    val template: DbJobTemplate by xdParent(DbJobTemplate::mapping)

    /** The [DbAttributeMapping]s that belong to this [DbEntityMapping]. */
    val attributes by xdChildren0_N(DbAttributeMapping::config)
}