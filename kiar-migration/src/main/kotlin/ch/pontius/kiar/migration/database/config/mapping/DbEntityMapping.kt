package ch.pontius.kiar.migration.database.config.mapping

import ch.pontius.kiar.api.model.config.mappings.MappingFormat
import ch.pontius.kiar.database.config.EntityMappings
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.Instant

/**
 * A storable entity mapping configuration as used by the KIAR Tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbEntityMapping(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbEntityMapping>(){
        fun migrate() {
            all().asSequence().forEach { dbEntityMapping ->
                EntityMappings.insert {
                    it[name] = dbEntityMapping.name
                    it[description] = dbEntityMapping.description
                    it[format] = MappingFormat.valueOf(dbEntityMapping.type.name)
                    it[created] = dbEntityMapping.createdAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                    it[modified] = dbEntityMapping.changedAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                }
            }
        }
    }

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