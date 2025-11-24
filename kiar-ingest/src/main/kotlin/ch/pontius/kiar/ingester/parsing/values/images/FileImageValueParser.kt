package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.parsing.values.images.providers.FileImageProvider
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuidOrNull
import com.sksamuel.scrimage.ImmutableImage
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Paths

/**
 * A [ValueParser] that converts a [String] (path) to a [ImmutableImage]. Involves reading the image from the file system.
 *
 * @author Ralph Gasser
 * @version 3.0.0
 */
class FileImageValueParser(override val mapping: AttributeMapping): ValueParser<List<ImmutableImage>> {

    /** Reads the search pattern from the parameters map.*/
    private val search: Regex? = this.mapping.parameters["search"]?.let { Regex(it) }

    /** Reads the replacement pattern from the parameters map.*/
    private val replace: String? = this.mapping.parameters["replace"]

    /** A source directory from which a path should be resolved. */
    private val source: String? = this.mapping.parameters["source"]

    /**
     * Parses the given [String] and resolves it into a [FileImageProvider] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse or null.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        if (value.isNullOrEmpty()) return

        /* Read path - apply Regex search/replace if needed. */
        val actualPath = if (this.search != null && this.replace != null) {
            value.trim().replace(this.search, this.replace)
        } else {
            value.trim()
        }

        /* Parse path. */
        val path = if (!this.source.isNullOrEmpty()) {
            Paths.get(this.source).resolve(actualPath)
        } else {
            Paths.get(actualPath)
        }

        /* Read file. */
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, FileImageProvider(into.uuidOrNull(), path, context))
        } else {
            into.setField(this.mapping.destination, FileImageProvider(into.uuidOrNull(), path, context))
        }
    }
}