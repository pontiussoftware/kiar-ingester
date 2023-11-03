package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import com.sksamuel.scrimage.ImmutableImage
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.io.IOException
import java.net.URL
import java.util.*

/**
 * A [ValueParser] that converts a [String] (ID) into a [ImmutableImage] that is downloaded via the zetcom / museumPlus API.
 *
 * @see http://docs.zetcom.com/framework-public/ws/ws-api-module.html#get-the-thumbnail-of-a-module-item-attachment
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MuseumplusImageParser(override val mapping: AttributeMapping): ValueParser<List<ImmutableImage>> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** Reads the search pattern from the parameters map.*/
    private val host: URL = this.mapping.parameters["host"]?.let { URL(it) }  ?: throw IllegalStateException("Host required but missing.")

    /** Reads the replacement pattern from the parameters map.*/
    private val username: String = this.mapping.parameters["username"]  ?: throw IllegalStateException("Username required but missing.")

    /** Reads the replacement pattern from the parameters map.*/
    private val password: String = this.mapping.parameters["password"] ?: throw IllegalStateException("Password required but missing.")

    /**
     * Parses the given [ImmutableImage].
     */
    override fun parse(value: String, into: SolrInputDocument) {
        /* Read IDs. */
        for (id in value.split(',').mapNotNull { it.trim().toIntOrNull() }) {
            val url = URL("${this.host}/ria-ws/application/module/Multimedia/${id}/thumbnail?size=EXTRA_EXTRA_LARGE")
            val image = this.downloadImage(url, this.username, this.password) ?: continue
            if (this.mapping.multiValued) {
                into.addField(this.mapping.destination, image)
            } else {
                into.setField(this.mapping.destination, image)
            }
        }
    }

    /**
     * Downloads the [ImmutableImage] for the provided [URL].
     *
     * @param url The [URL] to download the image from.
     * @param username The username used for authentication.
     * @param password The password used for authentication
     */
    private fun downloadImage(url: URL, username: String, password: String): ImmutableImage? = try {
        /* Set up basic authentication and open connection. */
        val connection = url.openConnection()
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray()))
        ImmutableImage.loader().fromStream(connection.inputStream)
    } catch (e: IOException) {
        LOGGER.error("Failed to download image from $url: ${e.message}")
        null
    }
}