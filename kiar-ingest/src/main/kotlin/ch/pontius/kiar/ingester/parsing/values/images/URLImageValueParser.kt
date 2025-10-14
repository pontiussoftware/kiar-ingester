package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.parsing.values.images.providers.URLImageProvider
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuidOrNull
import com.sksamuel.scrimage.ImmutableImage
import org.apache.solr.common.SolrInputDocument
import java.net.URI

/**
 * A [ValueParser] that converts a [URL] [String] [ImmutableImage] downloaded from a URL.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class URLImageValueParser(override val mapping: AttributeMapping): ValueParser<List<ImmutableImage>> {

    /** The separator used for splitting. */
    private val delimiter: String = this.mapping.parameters["delimiter"] ?: ","

    /** Reads the optional username from the parameters map. */
    private val username: String? = this.mapping.parameters["username"]

    /** Reads the optional password from the parameters map.*/
    private val password: String? = this.mapping.parameters["password"]

    /**
     * Parses the given [String] and resolves it into a [URLImageProvider] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        if (value.isNullOrEmpty()) return

        /* Read values. */
        val urls = value.split(this.delimiter).map {
            URI(it.trim()).toURL()
        }

        /* Process URls. */
        for (url in urls) {
            val provider = URLImageProvider(into.uuidOrNull(), url, context, this.username, this.password)
            if (this.mapping.multiValued) {
                into.addField(this.mapping.destination, provider)
            } else {
                into.setField(this.mapping.destination, provider)
            }
        }
    }
}