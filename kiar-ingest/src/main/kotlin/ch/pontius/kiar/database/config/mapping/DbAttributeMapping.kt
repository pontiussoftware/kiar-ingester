package ch.pontius.kiar.database.config.mapping

import ch.pontius.kiar.ingester.parsing.xml.AttributeMapping
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

/**
 * A storable attribute mapping configuration as used by the KIAR Tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbAttributeMapping(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbAttributeMapping>()

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

    /**
     * A convenience method used to convert this [DbEntityMapping] to a [AttributeMapping].
     *
     * Requires an ongoing transaction!
     *
     * @return [AttributeMapping]
     */
    fun toApi(): AttributeMapping {
        val parameters = this.parameters.asSequence().associate { it.key to it.value }
        return AttributeMapping(this.source, this.destination, this.parser.toApi(), this.required, this.multiValued, parameters)
    }
}