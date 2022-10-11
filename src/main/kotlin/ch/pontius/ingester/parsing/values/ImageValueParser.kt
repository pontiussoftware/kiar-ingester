package ch.pontius.ingester.parsing.values

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ImageValueParser: ValueParser<BufferedImage> {
    private var buffer: BufferedImage? = null
    /**
     * Parses the given [BufferedImage] and returns [T].
     */
    override fun parse(value: String) {
        val path = Paths.get(value)
        if (!Files.exists(path)) {
            throw IllegalArgumentException("Failed to load image $path; file does not exist.")
        }
        this.buffer = Files.newInputStream(path, StandardOpenOption.READ).use { ImageIO.read(it) }
    }

    override fun get(): BufferedImage? = this.buffer
}