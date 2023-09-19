package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.solr.common.SolrInputDocument

/**
 * [ValueParser] to convert a [String] to a [Double].
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class DoubleValueParser(override val mapping: AttributeMapping): ValueParser<Double> {

    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    override fun parse(value: String, into: SolrInputDocument) {
        val parsedValue = value.toDoubleOrNull() ?: return
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, parsedValue)
        } else {
            into.setField(this.mapping.destination, parsedValue)
        }
    }
}