package ch.pontius.kiar.ingester.parsing.values.images.providers

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.media.MediaProvider
import ch.pontius.kiar.ingester.processors.ProcessingContext
import com.sksamuel.scrimage.ImmutableImage
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * A [MediaProvider.Image] for the images addressed by a [URL].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class URLImageProvider(private val uuid: String?, private val url: URL, private val context: ProcessingContext, private val username: String? = null, private val password: String? = null): MediaProvider.Image {
    /**
     * Downloads the [ImmutableImage] for the provided [URL].
     */
    override fun open(): ImmutableImage? = try {
        /* Set up basic authentication and open connection. */
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000 /* Do not wait longer than 5 seconds for connection. */
        connection.readTimeout = 30000 /* Do not wait longer than 30 seconds for reading data. */
        try {
            if (this.username != null && this.password != null) {
                connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString("${this.username}:${this.password}".toByteArray()))
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { ImmutableImage.loader().fromStream(it) }
            } else {
                this.context.log(JobLog(context.jobId, this.uuid, null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to download image from '${this.url}'. Service responded with HTTP status ${connection.responseCode}."))
                null
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: IOException) {
        this.context.log(JobLog(context.jobId, this.uuid, null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to download image from '${this.url}'. An IO exception occurred: ${e.message}"))
        null
    }
}