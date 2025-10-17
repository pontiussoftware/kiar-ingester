package ch.pontius.kiar.migration.database.config.mapping

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdParent
import kotlinx.dnq.xdRequiredStringProp

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DbAttributeMappingParameters(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbAttributeMappingParameters>()

    /** The key of this [DbAttributeMappingParameters]. */
    var key by xdRequiredStringProp()

    /** The value of this [DbAttributeMappingParameters]. */
    var value by xdRequiredStringProp()

    /** The value of this [DbAttributeMappingParameters]. */
    var mapping: DbAttributeMapping by xdParent(DbAttributeMapping::parameters)
}