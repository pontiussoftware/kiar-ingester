package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.media.MediaProvider
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuidOrNull
import com.sksamuel.scrimage.ImmutableImage
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path
import java.util.*

/**
 * A [ValueParser] that converts a [String] (ID) into a [ImmutableImage] that is downloaded via the zetcom / museumPlus API.
 *
 * See [museumPlus API Documentation](http://docs.zetcom.com/framework-public/ws/ws-api-module.html#get-the-thumbnail-of-a-module-item-attachment)
 *
 * @author Ralph Gasser
 * @version 2.2.0
 */
class MuseumplusImageParser(override val mapping: AttributeMapping): ValueParser<List<ImmutableImage>> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }
    /** The separator used for splitting. */
    private val delimiter: String = this.mapping.parameters["delimiter"] ?: ","

    /** Reads the search pattern from the parameters map.*/
    private val host: URL = this.mapping.parameters["host"]?.let { URL(it) }  ?: throw IllegalStateException("Host required but missing.")

    /** Reads the replacement pattern from the parameters map.*/
    private val username: String = this.mapping.parameters["username"]  ?: throw IllegalStateException("Username required but missing.")

    /** Reads the replacement pattern from the parameters map.*/
    private val password: String = this.mapping.parameters["password"] ?: throw IllegalStateException("Password required but missing.")

    /** Reads the replacement pattern from the parameters map.*/
    private val mode: Mode = this.mapping.parameters["mode"]?.let {
        Mode.valueOf(it.uppercase())
    } ?: Mode.ID

    /**
     * Parses the given [String] and resolves it into a [MuseumplusImageProvider] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        if (value.isNullOrEmpty()) return

        /* Read values. */
        val urls = when (this.mode) {
            Mode.ID -> this.urlFromId(value)
            Mode.PATH -> this.urlFromPath(value)
        }

        /* Process URls. */
        for (url in urls) {
            val provider = MuseumplusImageProvider(into.uuidOrNull(), url, context)
            if (this.mapping.multiValued) {
                into.addField(this.mapping.destination, provider)
            } else {
                into.setField(this.mapping.destination, provider)
            }
        }
    }

    /**
     * Parses the given [String] and resolves it into a [URL] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @return [List] of [URL]s.
     */
    private fun urlFromId(value: String) = value.split(this.delimiter).mapNotNull {
        it.trim().toBigDecimalOrNull()?.toInt()
    }.map { id -> URL("${this.host}/ria-ws/application/module/Multimedia/${id}/thumbnail?size=EXTRA_EXTRA_LARGE") }

    /**
     * Parses the given [String] and resolves it into a [URL] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @return [List] of [URL]s.
     */
    private fun urlFromPath(value: String) = value.split(this.delimiter).map { URL("${this.host}/${it.trim()}") }

    /**
     * Returns the [Mode] for the provided [String].
     */
    private enum class Mode {
        ID, PATH
    }

    /**
     * A [MediaProvider.Image] for the images addressed by a [Path].
     */
    private inner class MuseumplusImageProvider(private val uuid: String?, private val url: URL, private val context: ProcessingContext): MediaProvider.Image {

        /**
         * Downloads the [ImmutableImage] for the provided [URL].
         */
        override fun open(): ImmutableImage? = try {
            /* Set up basic authentication and open connection. */
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString("${this@MuseumplusImageParser.username}:${this@MuseumplusImageParser.password}".toByteArray()))
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { ImmutableImage.loader().fromStream(it) }
                } else {
                    LOGGER.error("Failed to download image from $url; service responded with HTTP status ${connection.responseCode}.")
                    this.context.log(JobLog(context.jobId, this.uuid, null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to download image from '${this.url}'. Service responded with HTTP status ${connection.responseCode}."))
                    null
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            LOGGER.error("Failed to download image from $url; An exception occurred: ${e.message}")
            this.context.log(JobLog(context.jobId, this.uuid, null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to download image from '${this.url}'. An IO exception occurred: ${e.message}"))
            null
        }
    }
}