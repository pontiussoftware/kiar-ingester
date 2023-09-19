package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.solr.common.SolrInputDocument

/**
 * [ValueParser] to convert a [String] to a [Int].
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class IntegerValueParser(override val mapping: AttributeMapping): ValueParser<Int> {
    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    override fun parse(value: String, into: SolrInputDocument) {
        val parsedValue = value.toIntOrNull() ?: return
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, parsedValue)
        } else {
            into.setField(this.mapping.destination, parsedValue)
        }
    }
}