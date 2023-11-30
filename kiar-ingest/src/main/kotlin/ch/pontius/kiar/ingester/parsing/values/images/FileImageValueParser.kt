package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.media.MediaProvider
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import com.sksamuel.scrimage.ImmutableImage
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * A [ValueParser] that converts a [String] (path) to a [ImmutableImage]. Involves reading the image from the file system.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class FileImageValueParser(override val mapping: AttributeMapping): ValueParser<List<ImmutableImage>> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** Reads the search pattern from the parameters map.*/
    private val search: Regex? = this.mapping.parameters["search"]?.let { Regex(it) }

    /** Reads the replacement pattern from the parameters map.*/
    private val replace: String? = this.mapping.parameters["replace"]

    /**
     * Parses the given [ImmutableImage].
     */
    override fun parse(value: String, into: SolrInputDocument) {
        /* Read path - apply Regex search/replace if needed. */
        val actualPath = if (this.search != null && this.replace != null) {
            value.replace(this.search, this.replace)
        } else {
            value
        }

        /* Parse path and read file. */
        val path = Paths.get(actualPath)
        if (this.mapping.multiValued) {
            into.addField(mapping.destination, FileImageProvider(path))
        } else {
            into.setField(mapping.destination, FileImageProvider(path))
        }
    }

    /**
     * A [MediaProvider.Image] for the images addressed by a [Path].
     */
    private data class FileImageProvider(private val path: Path): MediaProvider.Image {
        override fun open(): ImmutableImage? = try {
            Files.newInputStream(this.path, StandardOpenOption.READ).use { ImmutableImage.loader().fromStream(it) }
        } catch (e: Throwable) {
            LOGGER.warn("Failed to decode image from path ${this.path}. An exception occurred: ${e.message}")
            null
        }
    }
}