package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO

/**
 * A [ValueParser] that converts a [String] (path) to a [BufferedImage]. Involves reading the image from the file system.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class FileImageValueParser(params: Map<String,String>): ValueParser<List<BufferedImage>> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The last [BufferedImage] extracted by this [FileImageValueParser]. */
    private var buffer: List<BufferedImage> = emptyList()

    /** Reads the search pattern from the parameters map.*/
    private val search: Regex? = params["search"]?.let { Regex(it) }

    /** Reads the replacement pattern from the parameters map.*/
    private val replace: String? = params["replace"]

    /**
     * Parses the given [BufferedImage].
     */
    override fun parse(value: String) {
        /* Read path - apply Regex search/replace if needed. */
        val actualPath = if (this.search != null && this.replace != null) {
            value.replace(this.search, this.replace)
        } else {
            value
        }

        /* Parse path and read file. */
        val path = Paths.get(actualPath)
        if (!Files.exists(path)) {
            LOGGER.warn("Failed to read image file $path: File does not exist")
        } else {
            this.buffer = try {
                listOf(Files.newInputStream(path, StandardOpenOption.READ).use { ImageIO.read(it) })
            } catch (e: Throwable) {
                LOGGER.warn("Failed to read image file $path: ${e.message}")
                emptyList()
            }
        }
    }

    override fun get(): List<BufferedImage> = this.buffer
}