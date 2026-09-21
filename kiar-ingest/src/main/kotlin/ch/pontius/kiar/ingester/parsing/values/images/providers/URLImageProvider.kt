package ch.pontius.kiar.ingester.parsing.values.images.providers

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.media.MediaProvider
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.utilities.SafeImageLoader
import com.sksamuel.scrimage.ImmutableImage
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.*

/**
 * A [MediaProvider.Image] that downloads an image from a [URL].
 *
 * URLs typically originate from the records being ingested, i.e., they are untrusted. This provider therefore only
 * fetches `http(s)` resources, only sends credentials to the [trustedHost] configured in the mapping, refuses to follow
 * redirects to a different host, and (unless [allowPrivateHosts] is set) refuses hosts that resolve to loopback,
 * link-local or private addresses, so that the ingest cannot be used to probe the server's internal network.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class URLImageProvider(
    private val uuid: String?,
    private val url: URL,
    private val context: ProcessingContext,
    private val username: String? = null,
    private val password: String? = null,
    private val trustedHost: String? = null,
    private val allowPrivateHosts: Boolean = false
): MediaProvider.Image {

    companion object {
        /** Maximum number of redirects followed. */
        private const val MAX_REDIRECTS = 3

        /** Maximum size of an image download in bytes (when the server announces a Content-Length). */
        private const val MAX_CONTENT_LENGTH = 100L * 1024 * 1024
    }

    override fun open(): ImmutableImage? = try {
        var current = this.url
        var image: ImmutableImage? = null
        var redirects = 0
        while (image == null) {
            /* Validate URL before connecting. */
            val rejection = this.validate(current)
            if (rejection != null) {
                this.warn("Refused to download image from '$current': $rejection")
                break
            }

            /* Open connection; credentials are only ever sent to the trusted host. */
            val connection = current.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000 /* Do not wait longer than 5 seconds for connection. */
            connection.readTimeout = 30000 /* Do not wait longer than 30 seconds for reading data. */
            connection.instanceFollowRedirects = false /* Redirects are validated manually. */
            try {
                if (this.username != null && this.password != null && this.isTrusted(current)) {
                    connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString("${this.username}:${this.password}".toByteArray()))
                }
                val status = connection.responseCode
                when {
                    status == HttpURLConnection.HTTP_OK -> {
                        if (connection.contentLengthLong > MAX_CONTENT_LENGTH) {
                            this.warn("Refused to download image from '$current': content length ${connection.contentLengthLong} exceeds limit of $MAX_CONTENT_LENGTH bytes.")
                            break
                        }
                        image = connection.inputStream.use { SafeImageLoader.load(it) }
                    }
                    status in 300..399 -> {
                        val location = connection.getHeaderField("Location")
                        if (location == null || ++redirects > MAX_REDIRECTS) {
                            this.warn("Failed to download image from '$current': too many or malformed redirects.")
                            break
                        }
                        current = current.toURI().resolve(location).toURL()
                    }
                    else -> {
                        this.warn("Failed to download image from '$current'. Service responded with HTTP status $status.")
                        break
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
        image
    } catch (e: Exception) {
        this.warn("Failed to download image from '${this.url}'. An exception occurred: ${e.message}")
        null
    }

    /**
     * Checks whether the given [URL] points to the configured, trusted host.
     */
    private fun isTrusted(url: URL): Boolean = this.trustedHost != null && url.host.equals(this.trustedHost, ignoreCase = true)

    /**
     * Validates the given [URL] and returns a reason for rejection, or null if the URL may be fetched.
     */
    private fun validate(url: URL): String? {
        if (url.protocol != "http" && url.protocol != "https") {
            return "unsupported scheme '${url.protocol}'."
        }
        if (url.host.isNullOrBlank()) {
            return "missing host."
        }
        if (this.isTrusted(url) || this.allowPrivateHosts) {
            return null
        }
        val addresses = try {
            InetAddress.getAllByName(url.host)
        } catch (e: Exception) {
            return "host could not be resolved."
        }
        if (addresses.any { it.isNonPublic() }) {
            return "host resolves to a non-public address."
        }
        return null
    }

    /**
     * Returns true if this [InetAddress] is a loopback, link-local, site-local, unspecified, multicast or IPv6 unique-local address.
     */
    private fun InetAddress.isNonPublic(): Boolean {
        if (this.isLoopbackAddress || this.isLinkLocalAddress || this.isSiteLocalAddress || this.isAnyLocalAddress || this.isMulticastAddress) {
            return true
        }
        val bytes = this.address
        return bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC /* IPv6 unique local addresses (fc00::/7). */
    }

    /**
     * Logs a warning to the [ProcessingContext].
     */
    private fun warn(message: String) {
        this.context.log(JobLog(this.context.jobId, this.uuid, null, JobLogContext.RESOURCE, JobLogLevel.WARNING, message))
    }
}
