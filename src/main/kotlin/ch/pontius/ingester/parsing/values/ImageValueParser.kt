package ch.pontius.ingester.parsing.values

import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.regex.Pattern
import javax.imageio.ImageIO

/**
 * A [ValueParser] that converts a [String] (path) to a [BufferedImage]. Involves reading the image from the file system.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ImageValueParser(params: Map<String,String>): ValueParser<BufferedImage> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The last [BufferedImage] extracted by this [ImageValueParser]. */
    private var buffer: BufferedImage? = null

    /** Reads the search pattern from the parameters map.*/
    private val search: Regex? = params["search"]?.let { Regex(it) }

    /** Reads the replacement pattern from the parameters map.*/
    private val replace: String? = params["replace"]

    /**
     * Parses the given [BufferedImage] and returns [T].
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
            LOGGER.warn("Failed to extract file $path: File does not exist")
        } else {
            this.buffer = Files.newInputStream(path, StandardOpenOption.READ).use { ImageIO.read(it) }
        }
    }

    override fun get(): BufferedImage? = this.buffer
}