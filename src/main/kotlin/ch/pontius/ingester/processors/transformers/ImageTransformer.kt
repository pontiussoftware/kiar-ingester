package ch.pontius.ingester.processors.transformers

import ch.pontius.ingester.config.TransformerConfig
import ch.pontius.ingester.processors.sources.Source
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_RAW
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.awt.image.BufferedImage
import java.nio.file.*
import javax.imageio.ImageIO


/**
 * A [Transformer] to operates on [SolrInputDocument]s, extracts raw image files, obtains a smaller preview and stores it.
 *
 * The [SolrInputDocument] is updated to contain the path to the new file.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ImageTransformer(override val input: Source<SolrInputDocument>, parameters: Map<String,String>): Transformer<SolrInputDocument, SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger(ImageTransformer::class.java)
    }

    /** Name of the [ImageTransformer]. Determines the name of the output folder. */
    private val name: String = parameters["name"] ?: throw IllegalArgumentException("Parameter 'name' is required by ImageTransformer.")

    /** Path to the folder the image derivatives should be deployed to.  */
    private val deployTo = parameters["deployTo"]?.let { Paths.get(it) } ?: throw IllegalArgumentException("Parameter 'deployTo' is required by ImageTransformer.")

    /** Path to the folder the image derivatives should be deployed to.  */
    private val maxSize = parameters["maxSize"]?.toIntOrNull() ?: 1280

    /**
     * Returns a [Flow] of this [ImageTransformer].
     */
    override fun toFlow(): Flow<SolrInputDocument> {
        /** The temporary directory to deploy images to. */
        if (!Files.exists(this.deployTo)) {
            throw IllegalArgumentException("Directory ${this.deployTo} does not exist!")
        }

        /* Prepare temporary directory. */
        val timestamp = System.currentTimeMillis()
        val ctxPath = this.deployTo.resolve(this.context)
        val dst = ctxPath.resolve(this.name)                  /* Destination directory, i.e., the directory that will contain all the generated images */
        val old = ctxPath.resolve("old-$timestamp")     /* Temporary location of the previous version of the destination directory (if exists). This is used to maintain atomicity. */
        val tmp = ctxPath.resolve("ingest-$timestamp")  /* Temporary location destination directory. This is used to maintain atomicity. */

        Files.createDirectories(tmp)
        return this.input.toFlow().map {
            try {
                if (it.containsKey(FIELD_NAME_RAW) && it.containsKey(FIELD_NAME_UUID)) {
                    val uuid = it[FIELD_NAME_UUID]!!.value as String
                    val images = it.getFieldValues(FIELD_NAME_RAW)
                    var counter = 1
                    for (original in images) {
                        if (original is BufferedImage) {
                            counter += 1
                            val actualPath = dst.resolve("${uuid}_%03d.jpg".format(counter))
                            val tmpPath = tmp.resolve("${uuid}_%03d.jpg".format(counter))
                            this.store(this.resize(original), tmpPath)
                            it.addField(this.name, "/" + this.deployTo.relativize(actualPath).toString())
                        }
                    }
                }
            } catch (e: Throwable) {
                LOGGER.warn("Error while processing image for ${it[FIELD_NAME_UUID]}; ${e.message}")
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
     * Resizes the provided [BufferedImage] to the size specified by [TransformerConfig].
     *
     * @param original The [BufferedImage] to resize.
     * @return The resized [BufferedImage]
     */
    private fun resize(original: BufferedImage): BufferedImage {
        val (targetWidth, targetHeight) = if (original.width > original.height) {
            this.maxSize to ((this.maxSize.toFloat() / original.width) * original.height).toInt()
        } else {
            ((this.maxSize .toFloat() / original.height) * original.width).toInt() to this.maxSize
        }
        val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val graphics2D = resized.createGraphics()
        graphics2D.drawImage(original, 0, 0, targetWidth, targetHeight, null)
        graphics2D.dispose()
        return resized
    }
}