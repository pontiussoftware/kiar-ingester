package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.api.model.config.image.ImageDeployment
import ch.pontius.kiar.api.model.config.image.ImageFormat.*
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.media.MediaProvider
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.*
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImageWriter
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.io.IOException
import java.nio.file.*
import java.util.*
import kotlin.Comparator

/**
 * A [Transformer] to operates on [SolrInputDocument]s, extracts raw image files, obtains a smaller preview and stores it.
 *
 * The [SolrInputDocument] is updated to contain the path to the new file.
 *
 * @author Ralph Gasser
 * @version 1.3.0
 */
class ImageDeployment(override val input: Source<SolrInputDocument>, private val deployments: List<ImageDeployment>, val test: Boolean = false): Transformer<SolrInputDocument, SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger(ImageDeployment::class.java)
    }

    /**
     * Returns a [Flow] of this [ImageDeployment].
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> {
        /** The temporary directory to deploy images to. */

        /* Prepare directories. */
        val writers = mutableMapOf<ImageDeployment, ImageWriter>()
        for (deployment in this.deployments) {
            val deployTo = Paths.get(deployment.path)
            if (!Files.exists(deployTo)) {
                throw IllegalArgumentException("Directory $deployTo does not exist!")
            }

            /* Create necessary directories. */
            Files.createDirectories(deployTo.resolve(context.participant).resolve(deployment.name))
            Files.createDirectories(deployTo.resolve(context.participant).resolve("${deployment.name}~tmp"))

            /* Prepare writers. */
            writers[deployment] = when (deployment.format) {
                JPEG -> JpegWriter(80, true)
                PNG -> PngWriter(9)
            }
        }

        /* Return flow for image deployment. */
        return this.input.toFlow(context).map {
            if (it.has(Field.RAW)) {
                val providers = LinkedList(it.getAll<MediaProvider.Image>(Field.RAW))
                var counter = 1
                for (provider in providers) {
                    val original = provider.open() ?: continue
                    for (deployment in this@ImageDeployment.deployments) {
                        val imageName = "${it.uuid()}_%03d.jpg".format(counter)
                        val deployTo = Paths.get(deployment.path)
                        val actual = deployTo.resolve(context.participant).resolve(deployment.name).resolve(imageName)
                        val tmp = deployTo.resolve(context.participant).resolve("${deployment.name}~tmp").resolve(imageName)
                        LOGGER.info("Deploying image (jobId = {}, docId = {}) {}.", context.jobId, it.uuid(), imageName)

                        /* Check size of image. If it's too small, issue a warning; otherwise, resize it. */
                        val resized = when {
                            original.width < deployment.maxSize && original.height < deployment.maxSize -> {
                                context.log(JobLog(null, it.uuid(), null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Image is smaller than specified maximum size (max = ${deployment.maxSize}, w = ${original.width}, h = ${original.height})."))
                                original
                            }
                            original.width > deployment.maxSize ->  original.scaleToWidth(deployment.maxSize)
                            else ->  original.scaleToHeight(deployment.maxSize)
                        }

                        /* Perform conversion. */
                        if (this@ImageDeployment.test || this.store(resized, writers[deployment]!!, tmp)) {
                            if (deployment.server == null) {
                                it.addField(deployment.name, deployTo.relativize(actual).toString())
                            } else {
                                it.addField(deployment.name, "${deployment.server}${deployTo.relativize(actual)}")
                            }
                            it.addField("${deployment.name}height_", resized.height)
                            it.addField("${deployment.name}width_", resized.width)
                        } else {
                            context.log(JobLog(null, it.uuid(), null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to create preview image for document."))
                        }
                    }
                    counter += 1
                }
                it.setField(Field.IMAGECOUNT, counter - 1)
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
     * Stores the provided [ImmutableImage] under the provided [Path].
     *
     * @param image The [ImmutableImage] to store.
     * @param writer The [ImageWriter] to use.
     * @param path The [Path] to store the image under.
     */
    private fun store(image: ImmutableImage, writer: ImageWriter, path: Path): Boolean = try {
        image.output(writer, path)
        true
    } catch (e: IOException) {
        LOGGER.error("Failed to save image $path due to IO exception: ${e.message}")
        false
    }
}