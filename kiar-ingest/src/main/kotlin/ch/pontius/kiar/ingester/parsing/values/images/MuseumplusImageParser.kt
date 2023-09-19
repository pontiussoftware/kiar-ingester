package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

/**
 * A [ValueParser] that converts a [String] (ID) into a [BufferedImage] that is downloaded via the zetcom / museumPlus API.
 *
 * @see http://docs.zetcom.com/framework-public/ws/ws-api-module.html#get-the-thumbnail-of-a-module-item-attachment
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class MuseumplusImageParser(params: Map<String,String>): ValueParser<List<BufferedImage>> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The last [BufferedImage] extracted by this [MuseumplusImageParser]. */
    private var buffer: List<BufferedImage> = emptyList()

    /** Reads the search pattern from the parameters map.*/
    private val host: URL = params["host"]?.let { URL(it) }  ?: throw IllegalStateException("Host required but missing.")

    /** Reads the replacement pattern from the parameters map.*/
    private val username: String = params["username"]  ?: throw IllegalStateException("Username required but missing.")

    /** Reads the replacement pattern from the parameters map.*/
    private val password: String = params["password"] ?: throw IllegalStateException("Password required but missing.")

    /**
     * Parses the given [BufferedImage].
     */
    override fun parse(value: String) {
        /* Read IDs. */
        val ids = value.split(',').map { it.trim().toInt() }
        val images = mutableListOf<BufferedImage>()
        for (id in ids) {
            val url = URL("${this.host.path}/ria-ws/application/module/Multimedia/${id}/thumbnail")
            val image = this.downloadImage(url, this.username, this.password)
            if (image != null) {
                images.add(image)
            }
        }
    }

    /**
     * The [List] of [BufferedImage]s parsed by the [ValueParser].
     *
     * @return [List] of [BufferedImage]s.
     */
    override fun get(): List<BufferedImage> = this.buffer

    /**
     * Downloads the [BufferedImage] for the provided [URL].
     *
     * @param url The [URL] to download the image from.
     * @param username The username used for authentication.
     * @param password The password used for authentication
     */
    private fun downloadImage(url: URL, username: String, password: String): BufferedImage? = try {
        /* Set up basic authentication and open connection. */
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray()))
        connection.connect()

        /* Download image. */
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.use {  input -> ImageIO.read(input) }
        } else {
            LOGGER.warn("Failed to download image from $url. HTTP-Status: ${connection.responseCode}")
            null
        }
    } catch (e: Throwable) {
        LOGGER.error("Failed to download image from $url: ${e.message}")
        null
    }
}