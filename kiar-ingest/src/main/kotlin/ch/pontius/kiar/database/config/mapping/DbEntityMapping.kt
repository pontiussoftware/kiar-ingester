package ch.pontius.kiar.database.config.mapping

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.config.MappingConfig
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

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

    /** The [DbAttributeMapping]s that belong to this [DbEntityMapping]. */
    val attributes by xdChildren0_N(DbAttributeMapping::mapping)

    /**
     * A convenience method used to convert this [DbEntityMapping] to a [MappingConfig]. Requires an ongoing transaction!
     *
     * @return [MappingConfig]
     */
    fun toApi() = EntityMapping(
        this.xdId,
        this.name,
        this.description,
        this.type.toApi(),
        this.attributes.asSequence().map { it.toApi() }.toList()
    )

    /**
     * A convenience method used to convert this [DbEntityMapping] to a [MappingConfig] without any attribute mappings. Requires an ongoing transaction!
     *
     * @return [MappingConfig]
     */
    fun toApiNoAttributes() = EntityMapping(
        this.xdId,
        this.name,
        this.description,
        this.type.toApi(),
        emptyList()
    )
}