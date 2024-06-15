package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import org.apache.solr.common.SolrInputDocument

/**
 * A (rather) trivial [ValueParser] implementation that returns a [String].
 *
 * @author Ralph Gasser
 * @version 2.1.0
 */
class StringValueParser(override val mapping: AttributeMapping): ValueParser<String> {
    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String, into: SolrInputDocument, context: ProcessingContext) {
        val cleaned = value.trim()
        if (cleaned.isNotBlank()) {
            if (this.mapping.multiValued) {
                into.addField(this.mapping.destination, cleaned)
            } else {
                into.setField(this.mapping.destination, cleaned)
            }
        }
    }
}