package ch.pontius.ingester.processors.transformers

import ch.pontius.ingester.config.ImageConfig
import ch.pontius.ingester.processors.sources.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO


/**
 * A [Transformer] to operates on [SolrInputDocument]s, extracts raw image files, obtains a smaller preview and stores it.
 *
 * The [SolrInputDocument] is updated to contain the path to the new file.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ImageTransformer(override val input: Source<SolrInputDocument>, val config: ImageConfig): Transformer<SolrInputDocument, SolrInputDocument> {

    companion object {
        val LOGGER = LogManager.getLogger(ImageTransformer::class.java)
    }

    /**
     * Returns a [Flow] of this [ImageTransformer].
     */
    override fun toFlow(): Flow<SolrInputDocument> {
        /** The temporary directory to deploy images to. */
        if (!Files.exists(this.config.deployTo)) {
            throw IllegalArgumentException("Directory ${this.config.deployTo} does not exist!")
        }

        /* Prepare temporary directory. */
        val timestamp = System.currentTimeMillis()
        val dst = this.config.deployTo.resolve(config.name)
        val old = this.config.deployTo.resolve("old-$timestamp")
        val tmp = this.config.deployTo.resolve("ingest-$timestamp")
        Files.createDirectories(tmp)

        return this.input.toFlow().map {
            try {
                if (it.containsKey("_raw_") && it.containsKey("uuid")) {
                    val uuid = it["uuid"]!!.value as String
                    val images = it.getFieldValues("_raw_")
                    var i = 1
                    for (original in images) {
                        if (original is BufferedImage) {
                            this.store( this.resize(original), tmp.resolve("${uuid}_%03d.jpg".format(i++)))
                        }
                    }
                    it.removeField("_raw_")
                }
            } catch (e: Throwable) {
            }
            it
        }.onCompletion {
            /* Move in new directory. */
            if (Files.exists(dst)) {
                Files.move(dst, old, StandardCopyOption.ATOMIC_MOVE)
                Files.move(tmp, dst, StandardCopyOption.ATOMIC_MOVE)
                Files.walk(old).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            } else {
                Files.move(tmp, dst, StandardCopyOption.ATOMIC_MOVE)
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Stores the provided [BufferedImage] under the provided [Path].
     *
     * @param image The [BufferedImage] to store.
     * @param path The [Path] to store the image under.
     */
    private fun store(image: BufferedImage, path: Path): Boolean {
        try {
            val os = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            return try {
                ImageIO.write(image, "JPEG", os)
                LOGGER.debug("Successfully stored image $path!")
                true
            } catch (e: Throwable) {
                LOGGER.error("Failed to save image $path due to exception: ${e.message}")
                false
            } finally {
                os.close()
            }
        } catch (e: Throwable) {
            LOGGER.error("Failed to create output stream for image $path due to exception: ${e.message}")
            return false
        }
    }

    /**
     * Resizes the provided [BufferedImage] to the size specified by [ImageConfig].
     *
     * @param original The [BufferedImage] to resize.
     * @return The resized [BufferedImage]
     */
    private fun resize(original: BufferedImage): BufferedImage {
        val (targetWidth, targetHeight) = if (original.width > original.height) {
            this.config.maxSize to ((this.config.maxSize .toFloat() / original.width) * original.height).toInt()
        } else {
            ((this.config.maxSize .toFloat() / original.height) * original.width).toInt() to this.config.maxSize
        }
        val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val graphics2D = resized.createGraphics()
        graphics2D.drawImage(original, 0, 0, targetWidth, targetHeight, null)
        graphics2D.dispose()
        return resized
    }
}