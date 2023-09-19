package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.solr.common.SolrInputDocument

/**
 * A (rather) trivial [ValueParser] implementation that returns an array of [String]s.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MultiStringValueParser(override val mapping: AttributeMapping): ValueParser<List<String>> {

    /** The separator used for splitting. */
    private val separator: String = this.mapping.parameters["separator"] ?: ", "

    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    override fun parse(value: String, into: SolrInputDocument) {
        val split = value.split(this.separator)
        if (split.isNotEmpty()) {
            if (this.mapping.multiValued) {
                split.forEach { into.addField(this.mapping.destination, it) }
            } else {
                into.setField(this.mapping.destination, split.first())
            }
        }
    }
}