package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import org.apache.solr.common.SolrInputDocument

/**
 * A (rather) trivial [ValueParser] implementation that returns an array of [String]s.
 *
 * @author Ralph Gasser
 * @version 2.2.0
 */
class MultiStringValueParser(override val mapping: AttributeMapping): ValueParser<List<String>> {

    /** The separator used for splitting. */
    private val separator: String = this.mapping.parameters["separator"] ?: ", "

    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse or null.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        if (value == null) return
        val split = value.split(this.separator)
        if (split.isNotEmpty()) {
            for (s in split) {
                val cleaned = s.trim()
                if (cleaned.isNotBlank()) {
                    if (this.mapping.multiValued) {
                        into.addField(this.mapping.destination, cleaned)
                    } else {
                        into.setField(this.mapping.destination, cleaned)
                    }
                }
            }
        }
    }
}