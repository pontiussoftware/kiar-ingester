package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO

/**
 * A [ValueParser] that converts a [String] (path) to a [BufferedImage]. Involves reading the image from the file system.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class FileImageValueParser(override val mapping: AttributeMapping): ValueParser<List<BufferedImage>> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** Reads the search pattern from the parameters map.*/
    private val search: Regex? = this.mapping.parameters["search"]?.let { Regex(it) }

    /** Reads the replacement pattern from the parameters map.*/
    private val replace: String? = this.mapping.parameters["replace"]

    /**
     * Parses the given [BufferedImage].
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
        val image = try {
            Files.newInputStream(path, StandardOpenOption.READ).use {
                into.addField(mapping.destination, ImageIO.read(it))
            }
        } catch (e: Throwable) {
            LOGGER.warn("Failed to read image file $path: ${e.message}")
            null
        }
        if (image != null) {
            if (this.mapping.multiValued) {
                into.addField(mapping.destination, image)
            } else {
                into.setField(mapping.destination, image)
            }
        }
    }
}