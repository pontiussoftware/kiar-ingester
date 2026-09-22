package ch.pontius.kiar.utilities

import com.sksamuel.scrimage.ImmutableImage
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/** The [KLogger] instance for [SafeImageLoader]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * Decodes images from untrusted sources (uploads, KIAR resources, remote URLs) with resource limits.
 *
 * A decoded image needs roughly four bytes per pixel, so a small compressed file declaring a huge canvas (a
 * "decompression bomb") can exhaust the heap and take the whole server down. This loader therefore reads the declared
 * dimensions from the image header first and refuses to decode anything above [MAX_PIXELS]. Streams are spooled to a
 * temporary file (bounded by [MAX_BYTES]) so that the header can be inspected before decoding.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object SafeImageLoader {

    /** Maximum number of pixels (width x height) an image may declare. 50 MP decodes to roughly 200 MB. */
    const val MAX_PIXELS: Long = 150_000_000L

    /** Maximum number of bytes read from a stream before giving up. */
    const val MAX_BYTES: Long = 100L * 1024 * 1024

    /** Thrown when an image exceeds the configured limits. */
    class ImageTooLargeException(message: String) : IOException(message)

    /**
     * Loads an image from a file after checking its declared dimensions.
     *
     * @param path The [Path] of the image file.
     * @return [ImmutableImage]
     * @throws ImageTooLargeException If the image declares more than [MAX_PIXELS] pixels.
     * @throws IOException If the image cannot be read or decoded.
     */
    fun load(path: Path): ImmutableImage {
        this.checkDimensions(path)
        return ImmutableImage.loader().fromPath(path)
    }

    /**
     * Loads an image from a stream. The stream is spooled to a temporary file (at most [MAX_BYTES]) so that the
     * header can be inspected before the image is decoded.
     *
     * @param stream The [InputStream] to read from; it is not closed by this method.
     * @return [ImmutableImage]
     * @throws ImageTooLargeException If the stream exceeds [MAX_BYTES] or the image declares more than [MAX_PIXELS] pixels.
     * @throws IOException If the image cannot be read or decoded.
     */
    fun load(stream: InputStream): ImmutableImage {
        val tmp = Files.createTempFile("kiar-image-", ".tmp")
        try {
            Files.newOutputStream(tmp).use { out ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BYTES) {
                        throw ImageTooLargeException("Image exceeds the maximum size of $MAX_BYTES bytes.")
                    }
                    out.write(buffer, 0, read)
                }
            }
            return this.load(tmp)
        } finally {
            try {
                Files.deleteIfExists(tmp)
            } catch (e: IOException) {
                logger.warn(e) { "Failed to delete temporary image file $tmp." }
            }
        }
    }

    /**
     * Reads the declared dimensions of the image at [path] via ImageIO (header only) and throws if they exceed [MAX_PIXELS].
     *
     * Formats without an ImageIO reader on the classpath cannot be checked and are passed through.
     */
    private fun checkDimensions(path: Path) {
        val input = ImageIO.createImageInputStream(path.toFile()) ?: return
        input.use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            if (!readers.hasNext()) {
                logger.debug { "No ImageIO reader for $path; dimensions cannot be checked before decoding." }
                return
            }
            val reader = readers.next()
            try {
                reader.setInput(stream, true, true)
                val width = reader.getWidth(0).toLong()
                val height = reader.getHeight(0).toLong()
                if (width <= 0 || height <= 0 || width * height > MAX_PIXELS) {
                    throw ImageTooLargeException("Image dimensions ${width}x${height} exceed the maximum of $MAX_PIXELS pixels.")
                }
            } finally {
                reader.dispose()
            }
        }
    }
}
