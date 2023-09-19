package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.api.model.config.image.ImageDeployment
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.config.TransformerConfig
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.*
import javax.imageio.ImageIO


/**
 * A [Transformer] to operates on [SolrInputDocument]s, extracts raw image files, obtains a smaller preview and stores it.
 *
 * The [SolrInputDocument] is updated to contain the path to the new file.
 *
 * @author Ralph Gasser
 * @version 1.3.0
 */
class ImageDeployment(override val input: Source<SolrInputDocument>, private val deployments: List<ImageDeployment>): Transformer<SolrInputDocument, SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger(ImageDeployment::class.java)
    }

    /**
     * Returns a [Flow] of this [ImageDeployment].
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> {
        /** The temporary directory to deploy images to. */

        /* Prepare directories. */
        for (deployment in this.deployments) {
            val deployTo = Paths.get(deployment.path)
            if (!Files.exists(deployTo)) {
                throw IllegalArgumentException("Directory $deployTo does not exist!")
            }

            /* Create necessary directories. */
            Files.createDirectories(deployTo.resolve(context.participant).resolve(deployment.name))
            Files.createDirectories(deployTo.resolve(context.participant).resolve("${deployment.name}~tmp"))
        }

        /* Return flow for image deployment. */
        return this.input.toFlow(context).map {
            if (it.has(Field.RAW)) {
                val images = it.getAll<BufferedImage>(Field.RAW)
                var counter = 1
                for (original in images) {
                    for (deployment in this@ImageDeployment.deployments) {
                        val deployTo = Paths.get(deployment.path)
                        val actual = deployTo.resolve(context.participant).resolve(deployment.name).resolve("${it.uuid()}_%03d.jpg".format(counter))
                        val tmp = deployTo.resolve(context.participant).resolve("${deployment.name}~tmp").resolve("${it.uuid()}_%03d.jpg".format(counter))

                        /* Check size of image. If it's too small, issue a warning. */
                        if (original.width < deployment.maxSize && original.height < deployment.maxSize) {
                            context.log.add(JobLog(null, it.uuid(), null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Image is smaller than specified maximum size (max = ${deployment.maxSize}, w = ${original.width}, h = ${original.height})."))
                        }

                        /* Perform conversion. */
                        val resized = this.resize(original, deployment.maxSize)
                        if (this.store(resized, tmp)) {
                            if (deployment.server == null) {
                                it.addField(deployment.name, deployTo.relativize(actual).toString())
                            } else {
                                it.addField(deployment.name, "${deployment.server}${deployTo.relativize(actual)}")
                            }
                            it.addField("${deployment.name}height_", resized.height)
                            it.addField("${deployment.name}width_", resized.width)
                        } else {
                            context.log.add(JobLog(null, it.uuid(), null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to create preview image for document."))
                        }
                    }
                    counter += 1
                }
                it.setField(Field.IMAGECOUNT, counter)
                it.removeField(Field.RAW) /* Clean-up to safe memory. */
            } else {
                it.setField(Field.IMAGECOUNT, 0)
            }
            it
        }.onCompletion {e ->
            for (deployment in this@ImageDeployment.deployments) {
                val dst = Paths.get(deployment.path).resolve(context.participant).resolve(deployment.name)
                val tmp = Paths.get(deployment.path).resolve(context.participant).resolve("${deployment.name}~tmp")
                val bak = Paths.get(deployment.path).resolve(context.participant).resolve("${deployment.name}~bak")
                if (e != null) {
                    /* Case 1: Cleanup after error. */
                    Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
                } else {
                    /* Case 2: Finalisation */
                    if (Files.exists(dst)) {
                        Files.move(dst, bak, StandardCopyOption.ATOMIC_MOVE)
                        Files.move(tmp, dst, StandardCopyOption.ATOMIC_MOVE)
                        Files.walk(bak).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
                    } else {
                        Files.move(tmp, dst, StandardCopyOption.ATOMIC_MOVE)
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Stores the provided [BufferedImage] under the provided [Path].
     *
     * @param image The [BufferedImage] to store.
     * @param path The [Path] to store the image under.
     */
    private fun store(image: BufferedImage, path: Path): Boolean = try {
        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use { os ->
            ImageIO.write(image, "JPEG", os)
            LOGGER.debug("Successfully stored image {}!", path)
            true
        }
    } catch (e: IOException) {
        LOGGER.error("Failed to save image $path due to IO exception: ${e.message}")
        false
    }

    /**
     * Resizes the provided [BufferedImage] to the size specified by [TransformerConfig].
     *
     * @param original The [BufferedImage] to resize.
     * @return The resized [BufferedImage]
     */
    private fun resize(original: BufferedImage, maxSize: Int): BufferedImage {
        val (targetWidth, targetHeight) = if (original.width > original.height) {
            maxSize to ((maxSize.toFloat() / original.width) * original.height).toInt()
        } else {
            ((maxSize .toFloat() / original.height) * original.width).toInt() to maxSize
        }
        val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val graphics2D = resized.createGraphics()
        graphics2D.drawImage(original, 0, 0, targetWidth, targetHeight, null)
        graphics2D.dispose()
        return resized
    }
}