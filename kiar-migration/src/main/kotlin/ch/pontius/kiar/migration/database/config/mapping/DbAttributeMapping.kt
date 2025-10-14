package ch.pontius.kiar.migration.database.config.mapping

import ch.pontius.kiar.api.model.config.mappings.ValueParser
import ch.pontius.kiar.database.config.AttributeMappings
import ch.pontius.kiar.database.config.EntityMappings
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * A storable attribute mapping configuration as used by the KIAR Tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbAttributeMapping(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbAttributeMapping>(){
        fun migrate() {
            all().asSequence().forEach { dbAttributeMapping ->
                AttributeMappings.insert {
                    it[entityMappingId] = EntityMappings.idByName(dbAttributeMapping.mapping.name) ?: throw IllegalStateException("Could not find entity mapping with name '${dbAttributeMapping.mapping.name}'.")
                    it[src] = dbAttributeMapping.source
                    it[destination] = dbAttributeMapping.destination
                    it[required] = dbAttributeMapping.required
                    it[multiValued] = dbAttributeMapping.multiValued
                    it[parser] = ValueParser.valueOf(dbAttributeMapping.parser.description)
                    it[parameters] = dbAttributeMapping.parameters.asSequence().map { m -> m.key to m.value }.toMap()
                }
            }
        }
    }

    /** The source of this [DbAttributeMapping]. Depending on the [DbFormat] of the [DbEntityMapping], this can hold an XPath (XML) or a JSON path. */
    var source by xdRequiredStringProp(unique = false, trimmed = true)

    /** The name of the destination (Apache Solr) field this [DbAttributeMapping] maps to. */
    var destination by xdRequiredStringProp(unique = false, trimmed = true)

    /** Flag indicating that this [DbAttributeMapping] maps to a required field. */
    var required by xdBooleanProp()

    /** Flag indicating that this [DbAttributeMapping] maps to a multivalued field. */
    var multiValued by xdBooleanProp()

    /** The [DbParser] this [DbAttributeMapping] should use. */
    var parser by xdLink1(DbParser)

    /** The [DbAttributeMappingParameters] used to configure this [DbAttributeMapping]. */
    val parameters by xdChildren0_N(DbAttributeMappingParameters::mapping)

    /** The [DbEntityMapping] this [DbAttributeMapping] belongs to.  */
    val mapping: DbEntityMapping by xdParent(DbEntityMapping::attributes)
}