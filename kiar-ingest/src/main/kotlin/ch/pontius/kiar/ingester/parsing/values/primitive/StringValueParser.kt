package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.solr.common.SolrInputDocument

/**
 * A (rather) trivial [ValueParser] implementation that returns a [String].
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class StringValueParser(override val mapping: AttributeMapping): ValueParser<String> {
    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    override fun parse(value: String, into: SolrInputDocument) {
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, value)
        } else {
            into.setField(this.mapping.destination, value)
        }
    }
}