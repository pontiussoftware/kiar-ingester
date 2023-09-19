package ch.pontius.kiar.ingester.parsing.values

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import org.apache.solr.common.SolrInputDocument


/**
 * Interface implemented by a [ValueParser] used to convert a [String] value to type [T]. Used for XML parsing.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface ValueParser<T> {

    /** The [AttributeMapping] this [ValueParser] belongs to. */
    val mapping: AttributeMapping

    /**
     * Parses the given [String] and updates this [ValueParser]'s state.
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    fun parse(value: String, into: SolrInputDocument)
}