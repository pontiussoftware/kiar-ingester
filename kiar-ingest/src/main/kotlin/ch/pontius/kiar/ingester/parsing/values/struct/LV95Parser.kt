package ch.pontius.kiar.ingester.parsing.values.struct

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import org.apache.solr.common.SolrInputDocument

/**
 * [ValueParser] to convert a [String] into a LV95 coordinate pair.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class LV95Parser(override val mapping: AttributeMapping): ValueParser<Double> {

    /** The separator used for splitting. */
    private val separator: String = this.mapping.parameters["separator"] ?: ","

    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    override fun parse(value: String, into: SolrInputDocument, context: ProcessingContext) {
        val coordinates = value.split(this.separator).mapNotNull { it.trim().toDoubleOrNull() }.toTypedArray()
        if (coordinates.size == 2) {
            into.setField(this.mapping.destination, "${coordinates[0]},${coordinates[1]}")
        }
    }
}