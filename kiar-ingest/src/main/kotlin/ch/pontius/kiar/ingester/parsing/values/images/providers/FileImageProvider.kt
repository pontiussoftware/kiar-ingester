package ch.pontius.kiar.ingester.parsing.values.images.providers

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.media.MediaProvider
import ch.pontius.kiar.ingester.processors.ProcessingContext
import com.sksamuel.scrimage.ImmutableImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * A [MediaProvider.Image] for the images addressed by a [Path].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class FileImageProvider(private val uuid: String, private val path: Path, private val context: ProcessingContext): MediaProvider.Image {
    override fun open(): ImmutableImage? = try {
        Files.newInputStream(this.path, StandardOpenOption.READ).use { ImmutableImage.loader().fromStream(it) }
    } catch (e: Throwable) {
        context.log(JobLog(context.jobId, this.uuid, null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to decode image from '${this.path}'. An exception occurred: ${e.message}"))
        null
    }
}